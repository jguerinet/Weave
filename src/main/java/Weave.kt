/*
 * Copyright 2013-2019 Julien Guerinet
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

package com.guerinet.weave

import com.guerinet.weave.config.ConstantsConfig
import com.guerinet.weave.config.Source
import com.guerinet.weave.config.StringsConfig
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.buffer
import okio.source
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
open class Weave {

    open val config: Configs by lazy { parseConfigJson() }

    open val platform by lazy {
        val platform = Platform.parse(config.platform)
        if (platform == null) {
            error("The platform must be Android, iOS, or Web")
            throw IllegalStateException()
        }
        platform
    }

    open val idKey by lazy { config.keyColumnName }

    open val headerKey by lazy { config.headerColumnName }

    open val platformsKey by lazy { config.platformsColumnName }

    open val constantsHeader = "List of Constants, auto-generated by Weave"

    /**
     * Weaves the Strings
     */
    open fun weave() {
        try {
            val stringsConfig = config.strings
            val constantsConfigs: List<ConstantsConfig>? = config.constants

            if (stringsConfig == null) {
                warning("No Strings config found")
            } else {
                verifyStringsConfigInfo(stringsConfig)
                val downloadedStrands = downloadAllStringStrands(stringsConfig)
                val verifiedIds = verifyKeys(downloadedStrands)
                val verifiedStrands = verifyStringStrands(stringsConfig, verifiedIds)
                writeStringStrands(stringsConfig, verifiedStrands)
                println("Strings parsing complete")
            }

            println()

            if (constantsConfigs == null) {
                warning("No Constants configs found")
            } else {
                constantsConfigs.forEach { constantsConfig ->
                    verifyConstantsConfigInfo(constantsConfig)
                    val downloadedStrands = downloadAllConstantsStrands(constantsConfig)
                    val verifiedIds = verifyKeys(downloadedStrands)
                    val verifiedStrands = verifyConstantsStrands(verifiedIds)
                    writeConstantsStrands(constantsConfig, verifiedStrands)
                    println("${constantsConfig.title} parsing complete")
                    println()
                }
            }
        } catch (e: IOException) {
            error("Weaving failed")
            e.printStackTrace()
        }
    }

    /**
     * Reads and returns the json String from the config file
     *
     * @throws IOException Thrown if there was an error opening or reading the config file
     */
    @Throws(IOException::class)
    open fun readFromConfigFile(): String {
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
        return configFile.source().buffer().readUtf8()
    }

    /**
     * Returns the parsed [Configs] from the read json String
     */
    open fun parseConfigJson(): Configs {
        val json = readFromConfigFile()
        return Json.nonstrict.parse(Configs.serializer(), json)
    }

    /* VERIFICATION */

    /**
     * Verifies the info is correct on a [StringsConfig] [config]
     */
    open fun verifyStringsConfigInfo(config: StringsConfig) {
        // Make sure there's at least one language
        if (config.languages.isEmpty()) {
            error("Please provide at least one language")
        }
    }

    /**
     * Verifies that all of the config info is present
     */
    open fun verifyConstantsConfigInfo(config: ConstantsConfig) {
        // Make sure there's a package for Android
        if (platform == Platform.ANDROID && config.packageName == null) {
            error("Please provide a package name for Android")
        }

        // Make sure there tagAlign Column is a multiple a 4
        if (config.tagsAlignColumn % 4 != 0) {
            error("tagsAlignColumn must be a multiple of 4")
        }
    }

    /* DOWNLOAD */

    /**
     * Attempts to download the Csv from the [source]. Returns the created [CsvListReader], null if an error occurred
     */
    open fun downloadCsv(source: Source): CsvListReader? {
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
    open fun parseHeaders(
        headers: Array<String?>,
        onColumn: (index: Int, header: String) -> Unit
    ): Pair<Int, Int> {
        // Keep track of which columns hold the keys and the platform
        var keyColumn = -1
        var platformColumn = -1

        headers.forEachIndexed { index, s ->
            if (s == null) {
                // Disregard the null headers
                return@forEachIndexed
            }

            when {
                // Note the key column if it matches the key key
                s.equals(idKey, ignoreCase = true) -> keyColumn = index
                // Note the platform column if it matches the platform key
                s.equals(platformsKey, ignoreCase = true) -> platformColumn = index
            }

            // Pass it to the lambda for the caller to do whatever with the result
            onColumn(index, s)
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
     *  [source]. Returns the list of parsed [BaseStrand]s
     */
    open fun parseCsv(
        source: Source,
        reader: CsvListReader,
        headers: Array<String?>,
        keyColumn: Int,
        platformColumn: Int,
        onLine: (lineNumber: Int, key: String, line: List<Any>) -> BaseStrand?
    ): List<BaseStrand> {
        // Create the list of Strings
        val strings = mutableListOf<BaseStrand>()

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
                currentLine = reader.read(*processors)
                continue
            }

            // Check if this is a header
            if (key.startsWith(headerKey)) {
                strings.add(BaseStrand(key.replace("###", "").trim(), source.title, lineNumber))

                // Increment the line number
                lineNumber++
                currentLine = reader.read(*processors)
                continue
            }

            if (platformColumn != -1) {
                // If there's a platform column, parse it and check that it's for the current platform
                val platforms = currentLine[platformColumn] as? String
                if (!isForPlatform(platforms)) {
                    // Increment the line number
                    lineNumber++
                    currentLine = reader.read(*processors)
                    continue
                }
            }

            // Delegate the parsing to the caller, add the resulting BaseStrand if there is one
            val baseString = onLine(lineNumber, key, currentLine)
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

    /**
     * Writes all of the Strings. Throws an [IOException] if there's an error
     */
    @Throws(IOException::class)
    open fun preparePrintWriter(path: String, title: String, write: (PrintWriter) -> Unit) {
        // Set up the writer for the given language, enforcing UTF-8
        val writer = PrintWriter(path, "UTF-8")

        // Write the Strings
        write(writer)

        // Show the outcome
        println("Wrote $title to file: $path")

        // Flush and close the writer
        writer.flush()
        writer.close()
    }

    /* STRING PARSING */

    /**
     * Downloads all of the Strings from all of the Urls. Throws an [IOException] if there are
     *  any errors downloading the Strings
     */
    @Throws(IOException::class)
    open fun downloadAllStringStrands(config: StringsConfig): List<BaseStrand> = config.sources
        .mapNotNull { downloadStrands(config, it) }
        .flatten()

    /**
     * Uses the given [source] to connect to a Url and download all of the Strings in the right
     *  format. This will return a list of [BaseStrand]s, null if there were any errors
     */
    open fun downloadStrands(config: StringsConfig, source: Source): List<BaseStrand>? {
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

        return parseCsv(source, reader, headers, keyColumn, platformColumn) { lineNumber, key, line ->
            // Add a new language String
            val languageString = LanguageStrand(key, source.title, lineNumber)

            // Go through the languages, add each translation
            config.languages.forEach {
                val currentLanguage = line[it.columnIndex] as? String
                if (currentLanguage != null) {
                    languageString.addTranslation(it.id, currentLanguage)
                }
            }
            languageString
        }
    }

    /**
     * Verifies that the keys are valid
     */
    open fun verifyKeys(strands: List<BaseStrand>): List<BaseStrand> {
        // Define the key checker pattern to make sure no illegal characters exist within the keys
        val keyChecker = Pattern.compile("[^A-Za-z0-9_]")

        // Get rid of all of the headers
        val filteredStrands = strands.filter { it is LanguageStrand || it is ConstantStrand }

        val toRemove = mutableListOf<BaseStrand>()

        // Check if there are any errors with the keys
        filteredStrands.forEach {
            // Check if there are any spaces in the keys
            if (it.key.contains(" ")) {
                error("${getLog(it)} contains a space in its key.")
            }

            if (keyChecker.matcher(it.key).find()) {
                error("${getLog(it)} contains some illegal characters.")
            }
        }

        // Remove all duplicates
        val verifiedStrands = strands.toMutableList()
        verifiedStrands.removeAll(toRemove)
        return verifiedStrands
    }

    /**
     * Verifies the [strands] by removing those who don't have translations and warning about the others
     *  that don't have all translations
     */
    open fun verifyStringStrands(config: StringsConfig, strands: List<BaseStrand>): List<BaseStrand> {
        val stringStrands = strands.mapNotNull { it as? LanguageStrand }
        val toRemove = mutableListOf<BaseStrand>()

        // Check if there are any duplicates
        for (i in stringStrands.indices) {
            val strand1 = stringStrands[i]

            for (j in i + 1 until stringStrands.size) {
                val strand2 = stringStrands[j]

                // If the keys are the same and it's not a header, show a warning and remove the older one
                if (strand1.key == strand2.key) {
                    warning("${getLog(strand1)} and ${getLog(strand2)} have the same key. The second one will be used")
                    toRemove.add(strand1)
                }
            }
        }

        val verifiedStrands = strands.toMutableList()
        verifiedStrands.removeAll(toRemove)
        toRemove.clear()

        stringStrands
            .forEach {
                val lineNumber = it.lineNumber
                val sourceName = it.sourceName
                if (it.translations.isEmpty()) {
                    // Show a warning message if there are no translations and remove it
                    warning("Line $lineNumber from $sourceName has no translations so it will not be parsed.")
                    toRemove.add(it)
                } else if (it.translations.size != config.languages.size) {
                    warning("Warning: Line $lineNumber from $sourceName is missing at least one translation")
                }
            }

        verifiedStrands.removeAll(toRemove)
        return verifiedStrands
    }

    /**
     * Writes all of the Strings for the different languages. Throws an [IOException] if there's
     *  an error
     */
    @Throws(IOException::class)
    open fun writeStringStrands(config: StringsConfig, strands: List<BaseStrand>) {
        // If there are no Strings to write, no need to continue
        if (strands.isEmpty()) {
            println("No Strings to write")
            return
        }

        // Go through each language, and write the file
        config.languages.forEach {
            preparePrintWriter(it.path, it.id) { writer ->
                writeStrands(writer, it, strands)
            }
        }
    }

    /**
     * Processes the Strings and writes them to a given file for the given [language]
     */
    open fun writeStrands(writer: PrintWriter, language: Language, strands: List<BaseStrand>) {
        // Header
        writeHeader(writer, language.id)

        val last = strands.last()

        // Go through the Strings
        strands.forEach {
            try {
                if (it !is LanguageStrand) {
                    // If we are parsing a header, write the value as a comment
                    writeComment(writer, it.key)
                } else {
                    writeString(writer, language, it, last == it)
                }
            } catch (e: Exception) {
                error(getLog(it), false)
                e.printStackTrace()
            }
        }

        // Footer
        writeFooter(writer)
    }

    /**
     * Writes the header to the current file using the [writer]. Uses the [language] for the Web object name
     */
    open fun writeHeader(writer: PrintWriter, language: String) {
        writer.apply {
            when (platform) {
                Platform.ANDROID -> println("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>")
                Platform.WEB -> {
                    println("{")
                    println("    \"en\" : {")
                }
                else -> return
            }
        }
    }

    /**
     * Writes a [comment] to the file
     */
    open fun writeComment(writer: PrintWriter, comment: String) {
        writer.apply {
            when (platform) {
                Platform.ANDROID -> println("\n    <!-- $comment -->")
                Platform.IOS -> println("\n/* $comment */")
                else -> return
            }
        }
    }

    /**
     * Writes a [strand] to the file within the current [language] within the file.
     *  Depending on the platform and whether this [isLastStrand], the String differs
     */
    open fun writeString(
        writer: PrintWriter,
        language: Language,
        strand: LanguageStrand,
        isLastStrand: Boolean
    ) {
        var string = strand.getString(language.id) ?: ""

        if (string.isBlank() && platform != Platform.WEB) {
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

        val key = strand.key
        when (platform) {
            Platform.ANDROID -> {
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
                    // Percentages
                    .replace(" % ", " %% ")

                // Check if this is an HTML String
                string = if (string.contains(HTML_START_TAG, ignoreCase = true)) {
                    // Don't format the greater than and less than symbols
                    string
                        .replace(HTML_START_TAG, "<![CDATA[", ignoreCase = true)
                        .replace(HTML_END_TAG, "]]>", ignoreCase = true)
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
            Platform.IOS -> {
                string = string
                    // Replace %s format specifiers with %@
                    .replace("%s", "%@")
                    .replace("\$s", "$@")
                    // Remove <html> </html>tags
                    .replace(HTML_START_TAG, "", ignoreCase = true)
                    .replace(HTML_END_TAG, "", ignoreCase = true)
                    // Percentages
                    .replace("%", "%%")

                writer.println("\"$key\" = \"$string\";")
            }
            Platform.WEB -> {
                // Remove <html> </html>tags
                string = string
                    .replace(HTML_START_TAG, "", ignoreCase = true)
                    .replace(HTML_END_TAG, "", ignoreCase = true)
                    // If there's just one placeholder, replace it with $1
                    .replace("%s", "$1")

                // If there are multiple placeholders, keep the formatting
                //  TODO Find a better way of doing this
                for (i in 1..10) {
                    string = string.replace("%$i\$s", "$$i")
                }

                writer.println("        \"$key\": \"$string\"${if (isLastStrand) "" else ","}")
            }
        }
    }

    /**
     * Writes the footer to the current file
     */
    open fun writeFooter(writer: PrintWriter) {
        writer.apply {
            when (platform) {
                Platform.ANDROID -> println("</resources>")
                Platform.WEB -> {
                    println("    }")
                    println("}")
                }
                else -> return
            }
        }
    }

    /* CONSTANTS PARSING */

    /**
     * Downloads all of the [ConstantStrand]s from all of the Urls. Throws an [IOException] if there are
     *  any errors downloading them
     */
    @Throws(IOException::class)
    open fun downloadAllConstantsStrands(config: ConstantsConfig): List<BaseStrand> = config.sources
        .mapNotNull { downloadConstantsStrands(config, it) }
        .flatten()

    /**
     * Downloads and returns the Constants strands from the [source] using the [config]
     */
    open fun downloadConstantsStrands(config: ConstantsConfig, source: Source): List<BaseStrand>? {
        val reader = downloadCsv(source) ?: return null

        val headers = reader.getHeader(true)
        var typeColumn = -1
        var tagColumn = -1

        // Keep track of which columns hold the keys and the platform
        val (keyColumn, platformColumn) = parseHeaders(headers) { index, header ->
            when {
                header.equals(config.typeColumnName, ignoreCase = true) -> typeColumn = index
                header.equals(config.keyColumnName, ignoreCase = true) -> tagColumn = index
            }
        }

        // Make sure we have the tag column
        if (tagColumn == -1) {
            error("Tag column with name ${config.keyColumnName} not found")
        }

        return parseCsv(source, reader, headers, keyColumn, platformColumn) { lineNumber, key, line ->
            val type = if (typeColumn != -1) {
                line[typeColumn] as? String
            } else {
                null
            }
            val tag = line[tagColumn] as? String

            when (tag) {
                null -> {
                    warning("Line $lineNumber has no tag and will not be parsed")
                    null
                }
                else -> ConstantStrand(key, source.title, lineNumber, type.orEmpty().trim(), tag.trim())
            }
        }
    }

    /**
     * Verifies the constants [strands] by ensuring that there are no duplicates (same type and same key)
     */
    open fun verifyConstantsStrands(strands: List<BaseStrand>): List<BaseStrand> {
        val constantsStrands = strands.mapNotNull { it as? ConstantStrand }
        val toRemove = mutableListOf<BaseStrand>()

        // Check if there are any duplicates
        for (i in constantsStrands.indices) {
            val strand1 = constantsStrands[i]

            for (j in i + 1 until constantsStrands.size) {
                val strand2 = constantsStrands[j]

                // If the keys are the same and the type is the same, show a warning and remove the older one
                if (strand1.key == strand2.key && strand1.type == strand2.type) {
                    warning(
                        "${getLog(strand1)} and ${getLog(strand2)} have the same key and type. " +
                                "The second one will be used"
                    )
                    toRemove.add(strand1)
                }
            }
        }

        val verifiedStrands = strands.toMutableList()
        verifiedStrands.removeAll(toRemove)
        return verifiedStrands
    }

    /**
     * Writes the constants Strings using the [config] data
     */
    open fun writeConstantsStrands(config: ConstantsConfig, strands: List<BaseStrand>) {
        // If there are no Strings to write, don't continue
        if (strands.isEmpty()) {
            warning("No ${config.title} Strings to write")
            return
        }

        // Get the object name by taking the last item on the path and removing the right suffix depending on platform
        val objectName = when (platform) {
            Platform.ANDROID -> config.path.split("/").last().removeSuffix(".kt")
            Platform.IOS -> config.path.split("/").last().removeSuffix(".swift")
            // No object name on web
            else -> ""
        }

        preparePrintWriter(config.path, config.title) { writer ->
            // Header
            writeConstantsHeader(writer, objectName, config.packageName, config.isTopLevelClassCreated)

            val sortedStrands = strands
                // Only write the Constants Strands, since they get pre-sorted so the comments make no sense
                .mapNotNull { it as? ConstantStrand }
                .toMutableList()

            // Get the Strands with no type
            val noTypeStrands = sortedStrands.filter { it.type.isBlank() }

            // Remove them from the original list
            sortedStrands.removeAll(noTypeStrands)

            // Write them
            noTypeStrands.forEachIndexed { index, constantsStrand ->
                writeConstantsString(
                    writer,
                    constantsStrand,
                    false,
                    config.tagsAlignColumn,
                    config.capitalizeVariables,
                    config.isTopLevelClassCreated,
                    index == noTypeStrands.lastIndex
                )
            }

            // Pull out the types from the Strands
            val types = sortedStrands.map { it.type }.toSet()

            // Go through the types, pull out the appropriate Strings and write them one by one
            types.forEachIndexed { index, type ->
                // Get the Strands for it
                val typeStrands = sortedStrands.filter { it.type.equals(type, ignoreCase = true) }

                // If there are no strands, don't continue
                if (typeStrands.isEmpty()) {
                    return@forEachIndexed
                }

                // Remove them from the original list
                sortedStrands.removeAll(typeStrands)

                // Write the header
                writeConstantsTypeHeader(writer, type, config.isTopLevelClassCreated)

                // Get the last strand
                val lastStrand = typeStrands.last()

                // Write the Strands
                typeStrands.forEach { strand ->
                    writeConstantsString(
                        writer,
                        strand,
                        true,
                        config.tagsAlignColumn,
                        config.capitalizeVariables,
                        config.isTopLevelClassCreated,
                        strand == lastStrand
                    )
                }

                writeConstantsTypeFooter(writer, config.isTopLevelClassCreated, index == types.indexOfLast { true })
            }

            // Footer
            writeConstantsFooter(writer, config.isTopLevelClassCreated)
        }
    }

    /**
     * Writes the header of an Constants type using the [writer] and the [typeName]. Uses [isTopLevelClassCreated] to
     *  format this correctly (indentation and commas on web)
     */
    open fun writeConstantsTypeHeader(writer: PrintWriter, typeName: String, isTopLevelClassCreated: Boolean) {
        writer.apply {
            if (platform == Platform.WEB || isTopLevelClassCreated) {
                // Add spacing for web Constants or mobile Constants if the top level class is created
                print("    ")
            }
            when (platform) {
                Platform.ANDROID -> println("object $typeName {")
                Platform.IOS -> println("enum $typeName {")
                Platform.WEB -> println("\"${typeName.toLowerCase()}\" : { ")
            }
        }
    }

    /**
     * Writes the footer of an Constants type using the [writer]. Uses [isTopLevelClassCreated] and [isLastType] to
     *  format this correctly (indentation and commas on web)
     */
    open fun writeConstantsTypeFooter(writer: PrintWriter, isTopLevelClassCreated: Boolean, isLastType: Boolean) {
        writer.apply {
            if (platform == Platform.WEB || isTopLevelClassCreated) {
                // Add spacing for web Constants or mobile Constants if the top level class is created
                print("    ")
            }
            print("}")

            if (!isLastType) {
                when (platform) {
                    // If we're on web and this isn't the last type, add a comma
                    Platform.WEB -> print(",")
                    // If we're on mobile and this isn't the last type, add an extra space
                    Platform.ANDROID, Platform.IOS -> println()
                }
            }

            println()
        }
    }

    /**
     * Writes the header for the Constants file, using the [writer]. This will create a new top level object using the
     *  [objectName] if [isTopLevelClassCreated] is true, and will use the [packageName] in Android
     */
    open fun writeConstantsHeader(
        writer: PrintWriter,
        objectName: String,
        packageName: String?,
        isTopLevelClassCreated: Boolean
    ) {
        writer.apply {
            when (platform) {
                Platform.ANDROID -> {
                    println("package $packageName")
                    println()
                    println("/**")
                    println(" * $constantsHeader")
                    println(" */")
                    // Only create the top level class if there should be one
                    if (isTopLevelClassCreated) {
                        println("object $objectName {")
                    }
                    println()
                }
                Platform.IOS -> {
                    println("//  $constantsHeader")
                    println()
                    // Only create the top level class if there should be one
                    if (isTopLevelClassCreated) {
                        println("class $objectName {")
                    }
                }
                Platform.WEB -> {
                    println("{")
                }
            }
        }
    }

    /**
     * Writes one [constantString] to the Constants file using the [writer]. This will nest the String if it [hasType]
     *  and depending on [isTopLevelClassCreated], determine what column to align this String to depending on
     *  [tagsAlignColumn], capitalize the key or not depending on [isCapitalized], and add the comma on Web depending
     *  on [isLast]
     */
    open fun writeConstantsString(
        writer: PrintWriter,
        constantString: ConstantStrand,
        hasType: Boolean,
        tagsAlignColumn: Int,
        isCapitalized: Boolean,
        isTopLevelClassCreated: Boolean,
        isLast: Boolean
    ) {
        try {
            val isWeb = platform == Platform.WEB
            // Capitalize the key for the mobile platforms unless isCapitalized is false
            val key = if (isWeb || !isCapitalized) {
                constantString.key
            } else {
                constantString.key.toUpperCase()
            }
            val tag = constantString.tag
            writer.apply {
                var stringLength = 0
                if (hasType) {
                    // If there's a type, add more spacing
                    print("    ")
                    stringLength += 4
                }

                if (platform == Platform.WEB || isTopLevelClassCreated) {
                    // If there's a top level class for mobile Constants, add more spacing
                    print("    ")
                    stringLength += 4
                }

                val string = when (platform) {
                    Platform.ANDROID -> "const val $key"
                    Platform.IOS -> "static let $key"
                    Platform.WEB -> ""
                    else -> throw IllegalArgumentException("Unknown platform: $platform")
                }

                stringLength += string.length
                val space = if (tagsAlignColumn - stringLength < 0) {
                    // 1 for the normal space between the variable name and the equals sign
                    1
                } else {
                    tagsAlignColumn - stringLength
                }

                val alignmentSpace = " ".repeat(space)

                when (platform) {
                    Platform.ANDROID -> println("$string$alignmentSpace= \"$tag\"")
                    Platform.IOS -> println("static let $key$alignmentSpace= \"$tag\"")
                    Platform.WEB -> {
                        print("\"$key\": \"$tag\"")
                        if (!isLast) {
                            print(",")
                        }
                        println()
                    }
                }
            }
        } catch (e: Exception) {
            error(getLog(constantString), false)
            e.printStackTrace()
        }
    }

    /**
     * Writes the footer if the Constants file using the [writer]. [isTopLevelClassCreated] tells us whether there
     *  should be a closing bracket on mobile
     */
    open fun writeConstantsFooter(writer: PrintWriter, isTopLevelClassCreated: Boolean) {
        if (platform == Platform.WEB || isTopLevelClassCreated) {
            // Only close the class if we are on web or there was a top level class
            writer.println("}")
        }
    }

    /* HELPERS */

    /**
     * Returns the header for a log message for a given [strand]
     */
    open fun getLog(strand: BaseStrand): String = "Line ${strand.lineNumber} from ${strand.sourceName}"

    /**
     * Prints an error [message], and terminates the program is [isTerminated] is true (defaults to true)
     */
    open fun error(message: String, isTerminated: Boolean = true) {
        println("Error: $message")
        if (isTerminated) {
            System.exit(-1)
        }
    }

    /**
     * Returns true if this [platformCsv] contains the [platform], false otherwise
     */
    open fun isForPlatform(platformCsv: String?): Boolean {
        val platforms = platformCsv
            ?.split(",")
            ?.mapNotNull { Platform.parse(it.trim().toLowerCase()) } ?: listOf()
        return platforms.isEmpty() || platforms.contains(platform)
    }

    /**
     * Prints a warning [message]
     */
    open fun warning(message: String) = println("Warning: $message")

    companion object {

        const val FILE_NAME = "weave-config.json"

        const val HTML_START_TAG = "<html>"

        const val HTML_END_TAG = "</html>"

        /**
         * Main entry function
         */
        @JvmStatic
        fun main(args: Array<String>) {
            Weave().weave()
        }
    }
}

/**
 * Convenience mapping to what is read from the config file. It can have a [strings] and/or an [constants].
 *  Also contains the [platform] this is for
 */
@Serializable
class Configs(
    val platform: String,
    @Optional val headerColumnName: String = "###",
    @Optional val keyColumnName: String = "key",
    @Optional val platformsColumnName: String = "platforms",
    @Optional val strings: StringsConfig? = null,
    @Optional val constants: List<ConstantsConfig>? = null
)

/**
 * Platform this data is for. This will determine the formatting
 */
enum class Platform {
    ANDROID,
    IOS,
    WEB;

    companion object {
        /**
         * Parses the [string] into a [Platform]. Returns null if none found
         */
        internal fun parse(string: String) = when {
            string.equals("Android", ignoreCase = true) -> ANDROID
            string.equals("iOS", ignoreCase = true) -> IOS
            string.equals("Web", ignoreCase = true) -> WEB
            else -> null
        }
    }
}
