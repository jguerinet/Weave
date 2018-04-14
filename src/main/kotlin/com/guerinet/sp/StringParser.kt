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

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.io.CsvListReader
import org.supercsv.prefs.CsvPreference
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.*
import java.util.regex.Pattern

/**
 * Main class, executes the main code for parsing the Google Docs file
 * @author Julien Guerinet
 * @since 1.0.0
 */
class StringParser {

    /* INSTANCE VARIABLES */

    /**
     * List of languages we are writing to
     */
    protected var languages: MutableList<Language>

    /**
     * List of Urls we are downloading from
     */
    protected var urls: MutableMap<String, String>

    /**
     * Platform we are downloading for
     */
    protected var platform: String? = null

    /**
     * List of Strings to write
     */
    protected var strings: MutableList<BaseString>

    /**
     * Writer to the current file we are writing to
     */
    protected var writer: PrintWriter

    @JvmStatic
    fun main(args: Array<String>) {
        StringParser().run()
    }

    /**
     * Runs the [StringParser]
     */
    fun run() {
        try {
            setup()
            readFromConfigFile()
            verifyConfigInfo()
            downloadAllStrings()
            verifyKeys()
            writeStrings()
            println("Strings parsing complete")
        } catch (e: IOException) {
            println("Error downloading Strings: ")
            e.printStackTrace()
        }

    }

    /**
     * Sets up the variables
     */
    protected fun setup() {
        // List of all of the languages the Strings are in
        languages = ArrayList()

        // List of Urls to get the Strings from
        urls = HashMap()

        // Platform this is for (null if not chosen)
        platform = null

        // List of Strings to write
        strings = ArrayList()
    }

    /**
     * Reads and parses the various pieces of info from the config file
     *
     * @throws IOException Thrown if there was an error opening or reading the config file
     */
    @Throws(IOException::class)
    protected fun readFromConfigFile() {
        var configReader: BufferedReader? = null
        try {
            configReader = BufferedReader(FileReader("../config.txt"))
        } catch (e: FileNotFoundException) {
            try {
                configReader = BufferedReader(FileReader("config.txt"))
            } catch (ex: FileNotFoundException) {
                println("Error: Config file not found")
                System.exit(-1)
            }

        }

        var line: String
        while ((line = configReader!!.readLine()) != null) {
            readConfigLine(line)
        }
        configReader!!.close()
    }

    protected fun readConfigLine(line: String) {
        if (line.startsWith(URL)) {
            // Get the Url: remove the header and separate the file name from the Url
            val urlString = line.replace(URL, "").trim({ it <= ' ' })
            val urlInfo = urlString.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

            if (urlInfo.size < 2) {
                println("Error: The following format has too few " +
                        "arguments for a Url: " + urlString)
                System.exit(-1)
            }

            if (!urlInfo[1].trim({ it <= ' ' }).isEmpty()) {
                // Save it as a new Url in the Url map
                urls[urlInfo[0].trim({ it <= ' ' })] = urlInfo[1].trim({ it <= ' ' })
            }
        } else if (line.startsWith(PLATFORM)) {
            // Get the platform: Remove the header
            val platformString = line.replace(PLATFORM, "").trim({ it <= ' ' })
            if (platformString.equals(ANDROID, ignoreCase = true)) {
                platform = ANDROID
            } else if (platformString.equals(IOS, ignoreCase = true)) {
                platform = IOS
            } else if (platformString.equals(WEB, ignoreCase = true)) {
                platform = WEB
            } else {
                // Not recognized
                println("Error: Platform must be either Android, iOS, or Web.")
                System.exit(-1)
            }
        } else if (line.startsWith(LANGUAGE)) {
            // Get the languages: remove the header and separate the language Id from the path
            val languageString = line.replace(LANGUAGE, "").trim({ it <= ' ' })
            val languageInfo = languageString.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

            if (languageInfo.size < 2) {
                println("Error: The following format has too few " +
                        "arguments for a language: " + languageString)
                System.exit(-1)
            }

            // Save it as a new language in the list of languages
            languages.add(Language(languageInfo[0].trim({ it <= ' ' }), languageInfo[1].trim({ it <= ' ' })))
        }
    }

    /**
     * Verifies that all of the config info is present
     */
    protected fun verifyConfigInfo() {
        // Make sure everything is set
        if (urls.isEmpty()) {
            println("Error: There must be at least one non-null Url")
            System.exit(-1)
        } else if (platform == null) {
            println("Error: You need to input a platform")
            System.exit(-1)
        } else if (languages.isEmpty()) {
            println("Error: You need to add at least one language")
            System.exit(-1)
        }

        // Make sure that we have a path per language
        for (language in languages) {
            if (language.path == null) {
                println("Error: Languages need a file path for Android and iOS")
                System.exit(-1)
            }
        }
    }

    /**
     * Downloads all of the Strings from all of the Urls
     *
     * @throws IOException Thrown if there is any error downloading the Strings
     */
    @Throws(IOException::class)
    protected fun downloadAllStrings() {
        for (urlKey in urls.keys) {
            // Go through the Urls and download all of the Strings
            val urlStrings = downloadStrings(urlKey, urls[urlKey])
                    ?: // Don't continue if there's an error downloading the Strings
                    return

            strings.addAll(urlStrings)
        }
    }

    /**
     * Connects to the given Url and downloads the Strings in the right format
     *
     * @param url       Url to connect to
     * @return List of Strings that were downloaded, null if there was an error
     * @throws IOException Thrown if there was any errors downloading the Strings
     */
    @Throws(IOException::class)
    protected fun downloadStrings(urlName: String, url: String): List<BaseString>? {
        // Connect to the URL
        println("Connecting to $url")
        val request = Request.Builder()
                .get()
                .url(url)
                .build()

        val response: Response
        try {
            response = OkHttpClient().setCache(null).newCall(request).execute()
        } catch (e: IOException) {
            // Catch the exception here to be able to continue a build even if we are not connected
            println("IOException while connecting to the URL")
            println("Error Message: " + e.message)
            return null
        }

        val responseCode = response.code()
        println("Response Code: $responseCode")

        if (responseCode != 200) {
            println("Error: Response Message: " + response.message())
            return null
        }

        // Set up the CSV reader
        val reader = CsvListReader(InputStreamReader(response.body().byteStream(),
                "UTF-8"), CsvPreference.EXCEL_PREFERENCE)

        // Keep track of which columns hold the keys and the platform
        var keyColumn = -1
        var platformColumn = -1

        // Get the header
        val header = reader.getHeader(true)

        for (i in header.indices) {
            val string = header[i]
                    ?: // Disregard null headers
                    continue

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
            for (language in languages) {
                if (string.trim { it <= ' ' }.equals(language.id, ignoreCase = true)) {
                    // If we find a match, set the column index for this language
                    language.columnIndex = i
                    break
                }
            }
        }

        // Make sure there is a key column
        if (keyColumn == -1) {
            println("Error: There must be a column marked 'key' with the String keys")
            System.exit(-1)
        }

        // Make sure that all languages have an index
        for (language in languages) {
            if (language.columnIndex == -1) {
                println("Error: " + language.id + " in " + urlName +
                        " does not have any translations.")
                System.exit(-1)
            }
        }

        // Create the list of Strings
        val strings = ArrayList<BaseString>()

        // Make a CellProcessor with the right length
        val processors = arrayOfNulls<CellProcessor>(header.size)

        // Go through each line of the CSV document into a list of objects.
        var currentLine: List<Any>
        // The current line number (start at 2 since 1 is the header)
        var lineNumber = 2
        while ((currentLine = reader.read(*processors)) != null) {
            // Get the key from the current line
            var key: String? = currentLine[keyColumn] as String

            // Check if there's a key
            if (key == null || key.trim { it <= ' ' }.isEmpty()) {
                println("Warning: Line " + lineNumber + " does not have a key and " +
                        "will not be parsed")

                // Increment the line number
                lineNumber++

                // Move on to the new String
                continue
            }

            // Trim the key before continuing
            key = key.trim { it <= ' ' }

            // Check if this is a header
            if (key.startsWith(HEADER_KEY)) {
                strings.add(BaseString(key.replace("###", "").trim { it <= ' ' }, urlName, lineNumber))

                // Increment the line number and continue
                lineNumber++
                continue
            }

            // Add a new language String
            val languageString = LanguageString(key, urlName, lineNumber)

            // Go through the languages, add each translation
            var allNull = true
            var oneNull = false
            for (language in languages) {
                val currentLanguage = currentLine[language.columnIndex] as String

                if (currentLanguage != null) {
                    allNull = false
                } else {
                    oneNull = true
                }

                languageString.addTranslation(language.id, currentLanguage)
            }

            // If there's a platform column , add them
            if (platformColumn != -1) {
                languageString.addPlatforms(currentLine[platformColumn] as String)
            }

            // Check if all of the values are null
            if (allNull) {
                // Show a warning message
                println("Warning: Line " + lineNumber + " from " + urlName +
                        " has no translations so it will not be parsed.")
            } else {
                if (oneNull) {
                    println("Warning: Line " + lineNumber + " from " + urlName +
                            " is missing at least one translation")
                }
                strings.add(languageString)
            }

            // Increment the line number
            lineNumber++
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

        // Check if there are any errors with the keys
        for (i in strings.indices) {
            val string1 = strings[i] as? LanguageString ?: continue

            // Skip headers for the checks

            // Check if there are any spaces in the keys
            if (string1.key.contains(" ")) {
                println("Error: " + getLog(string1) + " contains a space in its key.")
                System.exit(-1)
            }

            if (keyChecker.matcher(string1.key).find()) {
                println("Error: " + getLog(string1) +
                        " contains some illegal characters.")
                System.exit(-1)
            }

            // Check if there are any duplicates
            for (j in i + 1 until strings.size) {
                val string2 = strings[j]

                // If the keys are the same and it's not a header, show an error and stop
                if (string1.key == string2.key) {
                    println("Error: " + getLog(string1) + " and " + getLog(string2) +
                            " have the same key.")
                    System.exit(-1)
                }
            }
        }
    }

    /**
     * Writes all of the Strings for the different languages
     *
     * @throws IOException Thrown if any error happens
     */
    @Throws(IOException::class)
    protected fun writeStrings() {
        // Go through each language, and write the file
        for (language in languages) {
            // Set up the writer for the given language, enforcing UTF-8
            writer = PrintWriter(language.path, "UTF-8")

            writeStrings(language)

            println("Wrote " + language.id + " to file: " + language.path)

            writer.flush()
            writer.close()
        }
    }

    /**
     * Processes the Strings and writes them to a given file
     *
     * @param language Language we are writing for
     */
    protected fun writeStrings(language: Language) {
        // Header
        writeHeader()

        // Go through the Strings
        for (i in strings.indices) {
            val currentString = strings[i]

            try {
                if (currentString !is LanguageString) {
                    // If we are parsing a header, right the value as a comment
                    writeComment(currentString.key)
                    continue
                } else {
                    writeString(language, currentString, i == strings.size - 1)
                }
            } catch (e: Exception) {
                println("Error on " + getLog(currentString))
                e.printStackTrace()
            }

        }

        // Footer
        writeFooter()
    }

    /**
     * Writes the header to the current file
     */
    protected fun writeHeader() {
        when (platform) {
            ANDROID -> writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?> \n <resources>")
            WEB -> writer.println("{")
        }
    }

    /**
     * Writes a comment to the file
     *
     * @param comment  Comment String to process
     */
    protected fun writeComment(comment: String) {
        when (platform) {
            ANDROID -> writer.println("\n    <!-- $comment -->")
            IOS -> writer.println("\n/* $comment */")
        }
    }

    /**
     * Writes a String to the file
     *
     * @param language       Current language
     * @param languageString String to write
     * @param lastString     True if this is the last String of the file, false otherwise
     */
    protected fun writeString(language: Language, languageString: LanguageString,
            lastString: Boolean) {
        // Check that it's for the right platform
        if (!languageString.isForPlatform(platform!!)) {
            return
        }

        var string = languageString.getString(language.id)

        // Check if value is or null empty: if it is, continue
        if (string == null) {
            string = ""
        }

        if (string.trim { it <= ' ' }.isEmpty() && !platform!!.equals(WEB, ignoreCase = true)) {
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
        when (platform) {
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

                writer.println("    \"" + key + "\": \"" + string + "\"" + if (lastString) "" else ",")
            }
        }
    }

    /**
     * Writes the footer to the current file
     */
    protected fun writeFooter() {
        when (platform) {
            ANDROID -> writer.println("</resources>")
            WEB -> writer.println("}")
        }
    }

    /**
     * @param string String we want to log
     * @return Log message to use to designate the given String
     */
    protected fun getLog(string: BaseString): String {
        return "Line " + string.lineNumber + " from " + string.url
    }

    companion object {

        /** PLATFORM CONSTANTS */
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
    }
}