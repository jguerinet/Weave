/*
 * Copyright 2013-2018 Julien Guerinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.guerinet.sp

import com.guerinet.sp.config.Source
import com.guerinet.sp.config.StringConfig
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import config.BaseConfig
import kotlinx.serialization.json.JSON
import okio.Okio
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.io.CsvListReader
import org.supercsv.prefs.CsvPreference
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.regex.Pattern

/**
 * Main class, executes the main code for parsing the Google Docs file
 * @author Julien Guerinet
 * @since 1.0.0
 */
@Suppress("MemberVisibilityCanBePrivate")
open class StringParser {

    protected val configs = mutableListOf<BaseConfig>()

    /**
     * List of Strings to write
     */
    protected var strings = mutableListOf<BaseString>()

    /**
     * Writer to the current file we are writing to
     */
    protected lateinit var writer: PrintWriter

    /**
     * Runs the [StringParser]
     */
    fun run() {
        try {
            readFromConfigFile()
            verifyConfigInfo()
            configs.forEach {
                when (it) {
                    is StringConfig -> {
                        downloadAllStrings(it)
                        verifyKeys()
                        writeStrings(it)
                        println("Strings parsing complete")
                    }
                    // TODO Add Analytics work
                }
            }
        } catch (e: IOException) {
            println("Error running String Parser: ")
            e.printStackTrace()
        }

    }

    /**
     * Reads and parses the various pieces of info from the config file
     *
     * @throws IOException Thrown if there was an error opening or reading the config file
     */
    @Throws(IOException::class)
    protected fun readFromConfigFile() {
        // Find the config file
        var configFile = File(FILE_NAME)
        if (!configFile.exists()) {
            // If it's not in the directory, check one up
            configFile = File("../$FILE_NAME")
            if (!configFile.exists()) {
                error("Config File $FILE_NAME not found in current or parent directory")
            }
        }

        // Parse the Config from the file
        val config = JSON.parse(StringConfig.serializer(), Okio.buffer(Okio.source(configFile)).readUtf8())

        // Add the config to the list
        configs.add(config)
    }

    /**
     * Verifies that all of the config info is present
     */
    protected fun verifyConfigInfo() {
        configs.forEach {
            when (it) {
                is StringConfig -> verifyStringConfigInfo(it)
                // TODO Add analytics config checking
            }
        }
    }

    /**
     * Verifies the info is correct on a [StringConfig] [config]
     */
    protected fun verifyStringConfigInfo(config: StringConfig) {
        // Make sure everything is set
        if (!listOf(ANDROID, IOS, WEB).contains(config.platform)) {
            error("You need to input a valid platform (Android, iOS, Web)")
        } else if (config.languages.isEmpty()) {
            error("You need to add at least one language")
        }
    }

    /**
     * Downloads all of the Strings from all of the Urls. Throws an [IOException] if there are
     *  any errors downloading the Strings
     */
    @Throws(IOException::class)
    protected fun downloadAllStrings(config: StringConfig) {
        config.sources
            .mapNotNull {
                downloadStrings(config, it)
            }
            .forEach { strings.addAll(it) }
    }

    /**
     * Uses the given [source] to connect to a Url and download all of the Strings in the right
     *  format. This will return a list of [BaseString]s, null if there were any errors
     */
    protected fun downloadStrings(config: StringConfig, source: Source): List<BaseString>? {
        // Connect to the URL
        println("Connecting to ${source.url}")
        val request = Request.Builder()
            .get()
            .url(source.url)
            .build()

        val response: Response
        try {
            response = OkHttpClient().setCache(null).newCall(request).execute()
        } catch (e: IOException) {
            // Catch the exception here to be able to continue a build even if we are not connected
            println("IOException while connecting to the URL")
            println("Error Message: ${e.message}")
            return null
        }

        val responseCode = response.code()
        println("Response Code: $responseCode")

        if (responseCode != 200) {
            println("Error: Response Message: ${response.message()}")
            return null
        }

        // Set up the CSV reader
        val reader = CsvListReader(
            InputStreamReader(
                response.body().byteStream(),
                "UTF-8"
            ), CsvPreference.EXCEL_PREFERENCE
        )

        // Keep track of which columns hold the keys and the platform
        var keyColumn = -1
        var platformColumn = -1

        // Get the header
        val header = reader.getHeader(true)

        for (i in header.indices) {
            // Disregard null headers
            val string = header[i] ?: continue

            // Check if the String matches the key key
            if (string.equals(KEY, ignoreCase = true)) {
                keyColumn = i
                continue
            }

            // Check if the String matches the platform key
            if (string.equals(PLATFORMS, ignoreCase = true)) {
                platformColumn = i
                continue
            }

            // Check if the String matches any of the languages parsed
            val language = config.languages.find { string.trim().equals(it.id, ignoreCase = true) }
            language?.columnIndex = i
        }

        // Make sure there is a key column
        if (keyColumn == -1) {
            error("There must be a column marked 'key' with the String keys")
        }

        // Make sure that all languages have an index
        val language = config.languages.find { it.columnIndex == -1 }
        if (language != null) {
            error("Error: ${language.id} in ${source.title} does not have any translations.")
        }

        // Create the list of Strings
        val strings = mutableListOf<BaseString>()

        // Make a CellProcessor with the right length
        val processors = arrayOfNulls<CellProcessor>(header.size)

        // Go through each line of the CSV document into a list of objects.
        var currentLine = reader.read(*processors)

        // The current line number (start at 2 since 1 is the header)
        var lineNumber = 2

        while (currentLine != null) {
            // Get the key from the current line
            val key = (currentLine[keyColumn] as? String)?.trim()

            // Check if there's a key
            if (key == null || key.isBlank()) {
                println("Warning: Line $lineNumber does not have a key and will not be parsed")

                // Increment the line number
                lineNumber++

                // Next line
                currentLine = reader.read(*processors)

                continue
            }

            // Check if this is a header
            if (key.startsWith(HEADER_KEY)) {
                strings.add(BaseString(key.replace("###", "").trim(), source.title, lineNumber))

                // Increment the line number and continue
                lineNumber++
                continue
            }

            // Add a new language String
            val languageString = LanguageString(key, source.title, lineNumber)

            // Go through the languages, add each translation
            var allNull = true
            var oneNull = false
            config.languages.forEach {
                val currentLanguage = currentLine[it.columnIndex] as? String
                if (currentLanguage != null) {
                    allNull = false
                    languageString.addTranslation(it.id, currentLanguage)
                } else {
                    oneNull = true
                }
            }

            // If there's a platform column , add them
            if (platformColumn != -1) {
                languageString.addPlatforms(currentLine[platformColumn] as? String)
            }

            // Check if all of the values are null
            if (allNull) {
                // Show a warning message
                println(
                    "Warning: Line $lineNumber from ${source.title} has no translations " +
                            "so it will not be parsed."
                )
            } else {
                if (oneNull) {
                    println(
                        "Warning: Line $lineNumber from ${source.title} is missing at " +
                                "least one translation"
                    )
                }
                strings.add(languageString)
            }

            // Increment the line number
            lineNumber++

            // Next line
            currentLine = reader.read(*processors)
        }

        // Close the CSV reader
        reader.close()

        return strings
    }

    /**
     * Verifies that the keys are valid
     */
    protected fun verifyKeys() {
        // Define the key checker pattern to make sure no illegal characters exist within the keys
        val keyChecker = Pattern.compile("[^A-Za-z0-9_]")

        // Get rid of all of the headers
        val strings = this.strings.mapNotNull { it as? LanguageString }

        val toRemove = mutableListOf<LanguageString>()

        // Check if there are any errors with the keys
        for (i in strings.indices) {
            val string1 = strings[i]

            // Check if there are any spaces in the keys
            if (string1.key.contains(" ")) {
                error("${getLog(string1)} contains a space in its key.")
            }

            if (keyChecker.matcher(string1.key).find()) {
                error("${getLog(string1)} contains some illegal characters.")
            }

            // Check if there are any duplicates
            for (j in i + 1 until strings.size) {
                val string2 = strings[j]

                // If the keys are the same and it's not a header, show a warning and remove
                //  the older one
                if (string1.key == string2.key) {
                    println(
                        "Warning: ${getLog(string1)} and ${getLog(string2)} have the same " +
                                "key. The first one will be overwritten by the second one."
                    )
                    toRemove.add(string1)
                }
            }
        }

        // Remove all duplicates
        this.strings.removeAll(toRemove)
    }

    /**
     * Writes all of the Strings for the different languages. Throws an [IOException] if there's
     *  an error
     */
    @Throws(IOException::class)
    protected fun writeStrings(config: StringConfig) {
        // If there are no Strings to write, no need to continue
        if (strings.isEmpty()) {
            println("No Strings to write")
            return
        }

        // Go through each language, and write the file
        config.languages.forEach {
            // Set up the writer for the given language, enforcing UTF-8
            writer = PrintWriter(it.path, "UTF-8")

            writeStrings(config, it)

            println("Wrote ${it.id} to file: ${it.path}")

            writer.flush()
            writer.close()
        }
    }

    /**
     * Processes the Strings and writes them to a given file for the given [language]
     */
    protected fun writeStrings(config: StringConfig, language: Language) {
        // Header
        writeHeader(config)

        val last = strings.last()

        // Go through the Strings
        strings.forEach {
            try {
                if (it !is LanguageString) {
                    // If we are parsing a header, right the value as a comment
                    writeComment(config, it.key)
                } else {
                    writeString(config, language, it, last == it)
                }
            } catch (e: Exception) {
                println("Error on ${getLog(it)}")
                e.printStackTrace()
            }
        }

        // Footer
        writeFooter(config)
    }

    /**
     * Writes the header to the current file
     */
    protected fun writeHeader(config: BaseConfig) {
        when (config.platform) {
            ANDROID -> writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?> \n <resources>")
            WEB -> writer.println("{")
        }
    }

    /**
     * Writes a [comment] to the file
     */
    protected fun writeComment(config: BaseConfig, comment: String) {
        when (config.platform) {
            ANDROID -> writer.println("\n    <!-- $comment -->")
            IOS -> writer.println("\n/* $comment */")
        }
    }

    /**
     * Writes a [languageString] to the file within the current [language] within the file.
     *  Depending on the platform and whether this [isLastString], the String differs
     */
    protected fun writeString(
        config: StringConfig,
        language: Language,
        languageString: LanguageString,
        isLastString: Boolean
    ) {
        // Check that it's for the right platform
        if (!languageString.isForPlatform(config.platform)) {
            return
        }

        var string = languageString.getString(language.id)

        // Check if value is or null empty: if it is, continue
        if (string == null) {
            string = ""
        }

        if (string.isBlank() && config.platform != WEB) {
            // Skip over the empty values unless we're on Web
            return
        }

        // Unescaped quotations
        string = string.replace("\"", "\\" + "\"")

        // Copyright
        string = string.replace("(c)", "\u00A9")

        // New Lines
        string = string.replace("\n", "")

        val key = languageString.key
        when (config.platform) {
            ANDROID -> {
                // Ampersands
                string = string.replace("&", "&amp;")

                // Apostrophes
                string = string.replace("'", "\\'")

                // Unescaped @ signs
                string = string.replace("@", "\\" + "@")

                // Ellipses
                string = string.replace("...", "&#8230;")

                // Check if this is an HTML String
                if (string.contains("<html>") || string.contains("<HTML>")) {
                    // Don't format the greater than and less than symbols
                    string = string.replace("<html>", "<![CDATA[")
                    string = string.replace("</html>", "]]>")
                    string = string.replace("<HTML>", "<![CDATA[")
                    string = string.replace("</HTML>", "]]>")
                } else {
                    // Format the greater then and less than symbol otherwise
                    // Greater than
                    string = string.replace(">", "&gt;")

                    // Less than
                    string = string.replace("<", "&lt;")
                }

                // Add the XML tag
                writer.println("    <string name=\"$key\">$string</string>")
            }
            IOS -> {
                // Replace %s format specifiers with %@
                string = string.replace("%s", "%@")
                string = string.replace("\$s", "$@")

                // Remove <html> </html>tags
                string = string.replace("<html>", "")
                string = string.replace("</html>", "")
                string = string.replace("<HTML>", "")
                string = string.replace("</HTML>", "")

                writer.println("\"$key\" = \"$string\";")
            }
            WEB -> {
                // Remove <html> </html>tags
                string = string.replace("<html>", "")
                string = string.replace("</html>", "")
                string = string.replace("<HTML>", "")
                string = string.replace("</HTML>", "")

                writer.println("    \"$key\": \"$string\"${if (isLastString) "" else ","}")
            }
        }
    }

    /**
     * Writes the footer to the current file
     */
    protected fun writeFooter(config: BaseConfig) {
        when (config.platform) {
            ANDROID -> writer.println("</resources>")
            WEB -> writer.println("}")
        }
    }

    /**
     * Returns the header for a log message for a given [string]
     */
    protected fun getLog(string: BaseString): String {
        return "Line ${string.lineNumber} from ${string.url}"
    }

    protected fun error(message: String) {
        println("Error: $message")
        System.exit(-1)
    }

    companion object {

        private const val FILE_NAME = "sp-config.json"

        /* PLATFORM CONSTANTS */

        private const val ANDROID = "Android"
        private const val IOS = "iOS"
        private const val WEB = "Web"

        /* CSV Strings */

        /**
         * Designates which column holds the keys
         */
        private const val KEY = "key"

        /**
         * Designates which column holds the platforms
         */
        private const val PLATFORMS = "platforms"

        /**
         * Designates a header within the Strings document
         */
        private const val HEADER_KEY = "###"

        @JvmStatic
        fun main(args: Array<String>) {
            StringParser().run()
        }
    }
}