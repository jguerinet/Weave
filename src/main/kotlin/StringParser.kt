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
import config.AnalyticsConfig
import config.BaseConfig
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
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

    protected lateinit var config: Configs

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
            val stringsConfig = config.strings
            val analyticsConfig: AnalyticsConfig? = config.analytics

            if (stringsConfig == null) {
                warning("No Strings config found")
            } else {
                verifyStringConfigInfo(stringsConfig)
                downloadAllStrings(stringsConfig)
                verifyKeys()
                filterStrings(stringsConfig)
                writeStrings(stringsConfig)
                println("Strings parsing complete")
            }

            if (analyticsConfig == null) {
                warning("No Analytics config found")
            } else {
                verifyAnalyticsConfigInfo(analyticsConfig)
                // TODO
            }
        } catch (e: IOException) {
            error("StringParser failed")
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
        val config = JSON.parse(Configs.serializer(), Okio.buffer(Okio.source(configFile)).readUtf8())
        this.config = config
    }

    /* VERIFICATION */

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
     * Verifies that all of the config info is present
     */
    protected fun verifyAnalyticsConfigInfo(config: AnalyticsConfig) {
        // Make sure everything is set
        if (!listOf(ANDROID, IOS, WEB).contains(config.platform)) {
            error("You need to input a validation platform (Android, iOS, Web")
        }
    }

    /* DOWNLOAD */

    protected fun downloadCsv(source: Source): CsvListReader? {
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
            error("Message received: ${e.message}", false)
            return null
        }

        val responseCode = response.code()
        println("Response Code: $responseCode")

        if (responseCode != 200) {
            error("Response Message: ${response.message()}", false)
            return null
        }

        // Set up the CSV reader and return it
        return CsvListReader(InputStreamReader(response.body().byteStream(), "UTF-8"), CsvPreference.EXCEL_PREFERENCE)
    }

    /**
     * Parses the [headers] by letting the caller deal with specific cases with [onColumn], and returning the columns of
     *  the key and platform (-1 if not found)
     */
    protected fun parseHeaders(
        headers: Array<String?>,
        onColumn: (index: Int, header: String) -> Unit
    ): Pair<Int, Int> {
        // Keep track of which columns hold the keys and the platform
        var keyColumn = -1
        var platformColumn = -1

        headers.forEachIndexed { index, s ->
            when {
                // Disregard the null headers
                s == null -> return@forEachIndexed
                // Note the key column if it matches the key key
                s.equals(KEY, ignoreCase = true) -> keyColumn = index
                // Note the platform column if it matches the platform key
                s.equals(PLATFORMS, ignoreCase = true) -> platformColumn = index
                // Pass it to the lambda for the caller to do whatever with the result
                else -> onColumn(index, s)
            }
        }

        // Make sure there is a key column
        if (keyColumn == -1) {
            error("There must be a column marked 'key' with the String keys")
        }

        return Pair(keyColumn, platformColumn)
    }

    /**
     * Parses the csv from the [reader] using the [headers]. Determines the key and platforms using the [keyColumn] and
     *  [platformColumn], and delegates the parsing to the caller with [onLine]. Parses the headers itself using the
     *  [source]. Returns the list of parsed [BaseString]s
     */
    protected fun parseCsv(
        source: Source,
        reader: CsvListReader,
        headers: Array<String?>,
        keyColumn: Int,
        platformColumn: Int,
        onLine: (lineNumber: Int, key: String, platforms: String?, line: List<Any>) -> BaseString?
    ): List<BaseString> {
        // Create the list of Strings
        val strings = mutableListOf<BaseString>()

        // Make a CellProcessor with the right length
        val processors = arrayOfNulls<CellProcessor>(headers.size)

        // Go through each line of the CSV document into a list of objects.
        var currentLine = reader.read(*processors)

        // The current line number (start at 2 since 1 is the header)
        var lineNumber = 2

        while (currentLine != null) {
            // Get the key from the current line
            val key = (currentLine[keyColumn] as? String)?.trim()

            // Check if there's a key
            if (key.isNullOrBlank()) {
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

            // If there's a platform column, parse it
            val platforms = if (platformColumn != -1) {
                currentLine[platformColumn] as? String
            } else {
                null
            }

            // Delegate the parsing to the caller, add the resulting BaseString if there is one
            val baseString = onLine(lineNumber, key, platforms, currentLine)
            baseString?.apply { strings.add(this) }

            // Increment the line number
            lineNumber++

            // Next line
            currentLine = reader.read(*processors)
        }

        // Close the CSV reader
        reader.close()

        return strings
    }

    /* STRING PARSING */

    /**
     * Downloads all of the Strings from all of the Urls. Throws an [IOException] if there are
     *  any errors downloading the Strings
     */
    @Throws(IOException::class)
    protected fun downloadAllStrings(config: StringConfig) {
        config.sources
            .mapNotNull { downloadStrings(config, it) }
            .forEach { strings.addAll(it) }
    }

    /**
     * Uses the given [source] to connect to a Url and download all of the Strings in the right
     *  format. This will return a list of [BaseString]s, null if there were any errors
     */
    protected fun downloadStrings(config: StringConfig, source: Source): List<BaseString>? {
        val reader = downloadCsv(source) ?: return null

        // Get the header
        val headers = reader.getHeader(true)

        // Get the key and platform columns, and map the languages to the right indexes
        val (keyColumn, platformColumn) = parseHeaders(headers) { index, header ->
            // Check if the String matches any of the languages parsed
            val language = config.languages.find { header.trim().equals(it.id, ignoreCase = true) }
            language?.columnIndex = index
        }

        // Make sure that all languages have an index
        val language = config.languages.find { it.columnIndex == -1 }
        if (language != null) {
            error("${language.id} in ${source.title} does not have any translations.")
        }

        return parseCsv(source, reader, headers, keyColumn, platformColumn) { lineNumber, key, platforms, line ->
            // Add a new language String
            val languageString = LanguageString(key, source.title, lineNumber, platforms)

            // Go through the languages, add each translation
            var allNull = true
            var oneNull = false
            config.languages.forEach {
                val currentLanguage = line[it.columnIndex] as? String
                if (currentLanguage != null) {
                    allNull = false
                    languageString.addTranslation(it.id, currentLanguage)
                } else {
                    oneNull = true
                }
            }

            // Check if all of the values are null
            if (allNull) {
                // Show a warning message
                warning("Line $lineNumber from ${source.title} has no translations so it will not be parsed.")
                return@parseCsv null
            } else if (oneNull) {
                warning("Warning: Line $lineNumber from ${source.title} is missing at least one translation")
            }
            languageString
        }
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
                    warning("${getLog(string1)} and ${getLog(string2)} have the same key. The first one will be used")
                    toRemove.add(string1)
                }
            }
        }

        // Remove all duplicates
        this.strings.removeAll(toRemove)
    }

    /**
     * Filters out the Strings that are not for this platform
     */
    protected fun filterStrings(config: StringConfig) {
        strings = strings
            .filter { it !is LanguageString || it.isForPlatform(config.platform) }
            .toMutableList()
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
                error(getLog(it), false)
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
        var string = languageString.getString(language.id)

        // Check if value is or null empty: if it is, continue
        if (string == null) {
            string = ""
        }

        if (string.isBlank() && config.platform != WEB) {
            // Skip over the empty values unless we're on Web
            return
        }

        // Trim
        string = string
            .trim()
            // Unescaped quotations
            .replace("\"", "\\" + "\"")
            // Copyright
            .replace("(c)", "\u00A9")
            // New Lines
            .replace("\n", "")

        val key = languageString.key
        when (config.platform) {
            ANDROID -> {
                string = string
                    // Ampersands
                    .replace("&", "&amp;")
                    // Apostrophes
                    .replace("'", "\\'")
                    // Unescaped @ signs
                    .replace("@", "\\" + "@")
                    // Ellipses
                    .replace("...", "&#8230;")
                    // Dashes
                    .replace("-", "–")

                // Check if this is an HTML String
                string = if (string.contains("<html>", ignoreCase = true)) {
                    // Don't format the greater than and less than symbols
                    string
                        .replace("<html>", "<![CDATA[", ignoreCase = true)
                        .replace("</html>", "]]>", ignoreCase = true)
                } else {
                    // Format the greater then and less than symbol otherwise
                    string
                        // Greater than
                        .replace(">", "&gt;")
                        // Less than
                        .replace("<", "&lt;")
                }

                // Add the XML tag
                writer.println("    <string name=\"$key\">$string</string>")
            }
            IOS -> {
                string = string
                    // Replace %s format specifiers with %@
                    .replace("%s", "%@")
                    .replace("\$s", "$@")
                    // Remove <html> </html>tags
                    .replace("<html>", "", ignoreCase = true)
                    .replace("</html>", "", ignoreCase = true)

                writer.println("\"$key\" = \"$string\";")
            }
            WEB -> {
                // Remove <html> </html>tags
                string = string
                    .replace("<html>", "", ignoreCase = true)
                    .replace("</html>", "", ignoreCase = true)

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

    /* ANALYTICS PARSING */

    /**
     * Downloads all of the Analytics Strings from all of the Urls. Throws an [IOException] if there are
     *  any errors downloading them
     */
    @Throws(IOException::class)
    protected fun downloadAllAnalytics(config: AnalyticsConfig) {
        config.sources
            .mapNotNull { downloadAnalytics(config, it) }
            .forEach { strings.addAll(it) }
    }

    protected fun downloadAnalytics(config: AnalyticsConfig, source: Source): List<BaseString>? {
        val reader = downloadCsv(source) ?: return null

        val headers = reader.getHeader(true)
        var typeColumn = -1
        var tagColumn = -1

        // Keep track of which columns hold the keys and the platform
        val (keyColumn, platformColumn) = parseHeaders(headers) { index, header ->
            when {
                header.equals(config.typeColumnName, ignoreCase = true) -> typeColumn = index
                header.equals(config.tagColumnName, ignoreCase = true) -> tagColumn = index
            }
        }

        // Make sure we have the type and tag columns
        if (typeColumn == -1) {
            error("Type column with name ${config.typeColumnName} not found")
        }

        if (tagColumn == -1) {
            error("Tag column with name ${config.tagColumnName} not found")
        }

        return parseCsv(source, reader, headers, keyColumn, platformColumn) { lineNumber, key, platforms, line ->
            val type = line[typeColumn] as? String
            val tag = line[tagColumn] as? String

            when {
                type == null -> {
                    warning("Line $lineNumber has no type and will not be parsed")
                    null
                }
                tag == null -> {
                    warning("Line $lineNumber has no tag and will not be parsed")
                    null
                }
                else -> AnalyticsString(key, source.title, lineNumber, platforms, type, tag)
            }
        }
    }

    /* HELPERS */

    /**
     * Returns the header for a log message for a given [string]
     */
    protected fun getLog(string: BaseString): String = "Line ${string.lineNumber} from ${string.sourceName}"

    /**
     * Prints an error [message], and terminates the program is [isTerminated] is true (defaults to true)
     */
    protected fun error(message: String, isTerminated: Boolean = true) {
        println("Error: $message")
        if (isTerminated) {
            System.exit(-1)
        }
    }

    protected fun warning(message: String) = println("Warning: $message")

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

/**
 * Convenience mapping to what is read from the config file. It can have a [strings] and/or an [analytics]
 */
@Serializable
class Configs(@Optional val strings: StringConfig? = null, @Optional val analytics: AnalyticsConfig? = null)