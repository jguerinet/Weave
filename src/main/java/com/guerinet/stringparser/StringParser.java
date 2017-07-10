/*
 * Copyright 2013-2017 Julien Guerinet
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

package com.guerinet.stringparser;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class, executes the main code for parsing the Google Docs file
 * @author Julien Guerinet
 * @since 1.0.0
 */
public class StringParser {

    /* PLATFORM CONSTANTS */

    private static final int ANDROID = 0;
    private static final int IOS = 1;
    private static final int WEB = 2;

    /* FILE STRINGS */

    /**
     * Url of the Csv file in the config file
     */
    private static final String URL = "URL:";

    /**
     * Platform in the config file
     */
    private static final String PLATFORM = "Platform:";

    /**
     * One language in the config file
     */
    private static final String LANGUAGE = "Language:";

    /* CSV STRINGS */

    /**
     * Key String to designate which column holds the keys
     */
    private static final String KEY = "key";

    /**
     * Platforms String to designate which column holds the platforms
     */
    private static final String PLATFORMS = "platforms";

    /* ANDROID STRINGS */

    /**
     * Android XML Opener
     */
    private static final String XML_OPENER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

    /**
     * Android Resources Opener
     */
    private static final String RESOURCES_OPENER = "<resources>";

    /**
     * Android Resources Closer
     */
    private static final String RESOURCES_CLOSER = "</resources>";

    /* OTHER */

    /**
     * Key used for the header in the Strings document
     */
    private static final String HEADER_KEY = "###";

    public static void main(String[] args) throws IOException {
        // Keep a list of all of the languages the Strings are in
        List<Language> languages = new ArrayList<>();
        // The list of language Strings
        List<BaseString> strings = new ArrayList<>();
        // Url
        String url = null;
        // Platform this is for (-1 is not chosen)
        int platform = -1;

        // Read from the config file
        BufferedReader configReader = null;
        try {
            configReader = new BufferedReader(new FileReader("../config.txt"));
        } catch (FileNotFoundException e) {
            try {
                configReader = new BufferedReader(new FileReader("config.txt"));
            } catch (FileNotFoundException ex) {
                System.out.println("Error: Config file not found");
                System.exit(-1);
            }
        }

        String line;
        while ((line = configReader.readLine()) != null) {
            if (line.startsWith(URL)) {
                // Get the URL
                url = line.replace(URL, "").trim();
            } else if (line.startsWith(PLATFORM)) {
                // Get the platform: Remove the header
                String platformString = line.replace(PLATFORM, "").trim();
                if (platformString.equalsIgnoreCase("android")) {
                    platform = ANDROID;
                } else if (platformString.equalsIgnoreCase("ios")) {
                    platform = IOS;
                } else if (platformString.equalsIgnoreCase("web")) {
                    platform = WEB;
                } else {
                    // Not recognized
                    System.out.println("Error: Platform must be either Android, iOS, or Web.");
                    System.exit(-1);
                }
            } else if (line.startsWith(LANGUAGE)) {
                // Get the languages: remove the header and separate the language Id from the path
                String languageString= line.replace(LANGUAGE, "").trim();
                String[] languageInfo = languageString.split(",");

                if (languageInfo.length < 2) {
                    System.out.println("Error: The following format has too few " +
                        "arguments for a language: " + languageString);
                    System.exit(-1);
                }

                // Save it as a new language in the list of languages
                languages.add(new Language(languageInfo[0].trim(), languageInfo[1].trim()));
            }
        }
        configReader.close();

        // Make sure everything is set
        if (url == null || url.isEmpty()) {
            System.out.println("Error: URL Cannot be null");
            System.exit(-1);
        } else if (platform == -1) {
            System.out.println("Error: You need to input a platform");
            System.exit(-1);
        } else if (languages.isEmpty()) {
            System.out.println("Error: You need to add at least one language");
            System.exit(-1);
        }

        // Make sure that if we are on Android/iOS that we have a path per language
        if (platform == ANDROID || platform == IOS) {
            for (Language language : languages) {
                if (language.getPath() == null) {
                    System.out.println("Error: Languages need a file path for Android and iOS");
                    System.exit(-1);
                }
            }
        }

        // Connect to the URL
        System.out.println("Connecting to " + url);
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();

        Response response;
        try {
            response = new OkHttpClient().newCall(request).execute();
        } catch (IOException e) {
            // Catch the exception here to be able to continue a build even if we are not connected
            System.out.println("IOException while connecting to the URL");
            System.out.println("Error Message: " + e.getMessage());
            return;
        }

        int responseCode = response.code();
        System.out.println("Response Code: " + responseCode);

        if (responseCode != 200) {
            System.out.println("Error: Response Message: " + response.message());
            return;
        }

        // Set up the CSV reader
        CsvListReader reader = new CsvListReader(new InputStreamReader(
                response.body().byteStream(), "UTF-8"), CsvPreference.EXCEL_PREFERENCE);

        // Keep track of which columns hold the keys and the platform
        int keyColumn = -1;
        int platformColumn = -1;

        // Get the header
        final String[] header = reader.getHeader(true);

        for (int i = 0; i < header.length; i ++) {
            String string = header[i];

            // Check if the String matches the key key
            if (string.equalsIgnoreCase(KEY)) {
                keyColumn = i;
                continue;
            }

            // Check if the String matches the platform key
            if (string.equalsIgnoreCase(PLATFORMS)) {
                platformColumn = i;
                continue;
            }

            // Check if the String matches any of the languages parsed
            for (Language language : languages) {
                if (string.trim().equalsIgnoreCase(language.getId())) {
                    // If we find a match, set the column index for this language
                    language.setColumnIndex(i);
                    break;
                }
            }
        }

        // Make sure there is a key column
        if (keyColumn == -1) {
            System.out.println("Error: There must be a column marked 'key' with the String keys");
            System.exit(-1);
        }

        // Make sure that all languages have an index
        for (Language language : languages) {
            if (language.getColumnIndex() == -1) {
                System.out.println("Error: " + language.getId() +
                        " does not have any translations.");
                System.exit(-1);
            }
        }

        // Make a CellProcessor with the right length
        final CellProcessor[] processors = new CellProcessor[header.length];

        // Go through each line of the CSV document into a list of objects.
        List<Object> currentLine;
        // The current line number (start at 2 since 1 is the header)
        int lineNumber = 2;
        while ((currentLine = reader.read(processors)) != null) {
            // Get the key from the current line
            String key = (String) currentLine.get(keyColumn);

            // Check if there's a key
            if (key == null || key.trim().isEmpty()) {
                System.out.println("Warning: Line " + lineNumber + " does not have a key and " +
                        "will not be parsed");

                // Increment the line number
                lineNumber ++;

                // Move on to the new String
                continue;
            }

            // Trim the key before continuing
            key = key.trim();

            // Check if this is a header
            if (key.startsWith(HEADER_KEY)) {
                strings.add(new BaseString(key.replace("###", "").trim(), lineNumber));

                // Increment the line number and continue
                lineNumber ++;
                continue;
            }

            // Add a new language String
            LanguageString languageString = new LanguageString(key, lineNumber);

            // Go through the languages, add each translation
            boolean allNull = true;
            boolean oneNull = false;
            for (Language language : languages) {
                String currentLanguage = (String) currentLine.get(language.getColumnIndex());

                if (currentLanguage != null) {
                    allNull = false;
                } else {
                    oneNull = true;
                }

                languageString.addTranslation(language.getId(), currentLanguage);
            }

            // If there's a platform column , add them
            if (platformColumn != -1) {
                languageString.addPlatforms((String) currentLine.get(platformColumn));
            }

            // Check if all of the values are null
            if (allNull) {
                // Show a warning message
                System.out.println("Warning: Line " + lineNumber + " has no translations so it " +
                        "will not be parsed.");
            } else {
                if (oneNull) {
                    System.out.println("Warning: Line " + lineNumber + " is missing at least one " +
                            "translation");
                }
                strings.add(languageString);
            }

            // Increment the line number
            lineNumber++;
        }

        // Close the CSV reader
        reader.close();

        // Check if there are any errors with the keys
        for (int i = 0; i < strings.size(); i ++) {
            BaseString string1 = strings.get(i);

            // Skip headers for the checks
            if (!(string1 instanceof LanguageString)) {
                continue;
            }

            // Check if there are any spaces in the keys
            if (string1.getKey().contains(" ")) {
                System.out.println("Error: Line " + string1.getLineNumber() +
                        " contains a space in its key.");
                System.exit(-1);
            }

            if (string1.getKey().contains("-")) {
                System.out.println("Error: Line " + string1.getLineNumber() +
                        " contains a dash in its key.");
                System.exit(-1);
            }

            // Check if there are any duplicates
            for (int j = i + 1; j < strings.size(); j ++) {
                BaseString string2 = strings.get(j);

                // If the keys are the same and it's not a header, show an error and stop
                if (string1.getKey().equals(string2.getKey())) {
                    System.out.println("Error: Lines " + string1.getLineNumber() + " and " +
                            string2.getLineNumber() + " have the same key.");
                    System.exit(-1);
                }
            }
        }

        // Go through each language, and write the file
        PrintWriter writer;
        for (Language language : languages) {
            // Set up the writer for the given language, enforcing UTF-8
            writer = new PrintWriter(language.getPath(), "UTF-8");

            if (platform == ANDROID) {
                processAndroidStrings(writer, language, strings);
            } else if (platform == IOS) {
                processIOSStrings(writer, language, strings);
            } else {
                processWebStrings(writer, language, strings);
            }

            System.out.println("Wrote " + language.getId() + " to file: " + language
                    .getPath());

            writer.close();
        }

        // Exit message
        System.out.println("Strings parsing complete");
    }

    /* HELPERS */

    /**
     * Processes the String by doing some checks and preparing some special characters
     *
     * @param string   The LanguageString object
     * @param language The language to parse the String for
     * @return The formatted String for the given language and platform
     */
    private static String processString(BaseString string, Language language) {
        String value;
        // Check if we are parsing a header, use the key for the value
        if (!(string instanceof LanguageString)) {
            value = string.getKey();
        } else {
            value = ((LanguageString) string).getString(language.getId());
        }

        // Check if value is or null empty: if it is, return null
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Process the value with the general methods first:

        // Unescaped quotations
        value = value.replace("\"", "\\" + "\"");

        // Copyright
        value = value.replace("(c)", "\u00A9");

        // New Lines
        value = value.replace("\n", "");

        return value;
    }

    /**
     * Processes the list of parsed Strings into the Android XML document
     *
     * @param writer   The writer to use to write to the file
     * @param language The language to parse the Strings for
     * @param strings  The list of Strings
     * @throws FileNotFoundException Thrown if the file we should be writing to is not found
     * @throws UnsupportedEncodingException Should never be thrown
     */
    private static void processAndroidStrings(PrintWriter writer, Language language,
            List<BaseString> strings) throws FileNotFoundException, UnsupportedEncodingException {
        // Add the header
        writer.println(XML_OPENER);
        writer.println(RESOURCES_OPENER);

        // Go through the strings
        for (BaseString currentString : strings) {
            try {
                // Process the String
                String string = processString(currentString, language);

                // If the String is null, continue
                if (string == null) {
                    continue;
                }

                // Add initial indentation
                String xmlString = "    ";

                // Check if it's a header section
                if (!(currentString instanceof LanguageString)) {
                    // Leave a space before it, add the header as a comment
                    xmlString = "\n" + xmlString + "<!-- " + string + " -->";
                } else {
                    // If not, treat is as a normal string

                    /* Character checks */
                    // Ampersands
                    string = string.replace("&", "&amp;");

                    // Apostrophes
                    string = string.replace("'", "\\'");

                    // Unescaped @ signs
                    string = string.replace("@", "\\" + "@");

                    // Ellipses
                    string = string.replace("...", "&#8230;");

                    // Check if this is an HTML String
                    if (string.contains("<html>") || string.contains("<HTML>")) {
                        // Don't format the greater than and less than symbols
                        string = string.replace("<html>", "<![CDATA[");
                        string = string.replace("</html>", "]]>");
                        string = string.replace("<HTML>", "<![CDATA[");
                        string = string.replace("</HTML>", "]]>");
                    } else {
                        // Format the greater then and less than symbol otherwise
                        // Greater than
                        string = string.replace(">", "&gt;");

                        // Less than
                        string = string.replace("<", "&lt;");
                    }

                    if (string.isEmpty()) {
                        System.out.println("Warning: Line " + currentString.getLineNumber() +
                                " doesn't have a translation for " + language.getId());
                    }

                    // Add the XML tag
                    xmlString = xmlString + "<string name=\"" + currentString.getKey() + "\">" +
                            string + "</string>";
                }

                // Write the String
                writer.println(xmlString);
            } catch (Exception e) {
                System.out.println("Error on Line " + currentString.getLineNumber());
                e.printStackTrace();
            }
        }

        // Add the resources closing to android files
        writer.println(RESOURCES_CLOSER);
    }

    /**
     * Processes the list of parsed Strings into the iOS Strings document
     *
     * @param writer   The writer to use to write to file
     * @param language The language to parse the Strings for
     * @param strings  The list of Strings
     * @throws FileNotFoundException Thrown if the file we should be writing to isn't found
     * @throws UnsupportedEncodingException Should never be thrown
     */
    private static void processIOSStrings(PrintWriter writer, Language language,
            List<BaseString> strings) throws FileNotFoundException, UnsupportedEncodingException {
        // Go through the strings
        for (BaseString currentString : strings) {
            try {
                // Get the processed String
                String string = processString(currentString, language);

                // If the String is null, there is no value so do not add it
                if (string == null) {
                    continue;
                }

                // Replace %s format specifiers with %@
                string = string.replace("%s", "%@");
                string = string.replace("$s", "$@");

                // Remove <html> </html>tags
                string = string.replace("<html>", "");
                string = string.replace("</html>", "");
                string = string.replace("<HTML>", "");
                string = string.replace("</HTML>", "");

                if (string.isEmpty()) {
                    System.out.println("Warning: Line " + currentString.getLineNumber() +
                            " doesn't have a translation for " + language.getId());
                }

                // Check if it's a header section
                if (!(currentString instanceof LanguageString)) {
                    string = "\n" + "/*  " + string + " */";
                } else {
                    // If not, treat is as a normal string
                    string = "\"" + currentString.getKey() + "\" = \"" + string + "\";";
                }

                writer.println(string);
            } catch (Exception e) {
                System.out.println("Error on Line " + currentString.getLineNumber());
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes the list of parsed Strings into the Web Strings document
     *
     * @param writer   The writer to use to write to file
     * @param language The language to parse the Strings for
     * @param strings  The list of Strings
     * @throws FileNotFoundException Thrown if the file we should be writing to isn't found
     * @throws UnsupportedEncodingException Should never be thrown
     */
    private static void processWebStrings(PrintWriter writer, Language language,
            List<BaseString> strings) throws FileNotFoundException, UnsupportedEncodingException {

        // Open the JSON object
        writer.println("{");

        // Go through the strings
        for (int i = 0; i < strings.size(); i ++) {
            BaseString string = strings.get(i);

            // We don't deal with header strings
            if (!(string instanceof LanguageString)) {
                continue;
            }

            try {
                // Open the object
                writer.print("    \"" + string.getKey() + "\": ");

                String value = ((LanguageString) string).getString(language.getId());
                if (value == null) {
                    value = "";
                }

                // Unescaped quotes
                value = value.replace("\"", "\\" + "\"");

                // New Lines
                value = value.replace("\n", "");

                // Remove <html> </html>tags
                value = value.replace("<html>", "");
                value = value.replace("</html>", "");
                value = value.replace("<HTML>", "");
                value = value.replace("</HTML>", "");

                if (value.isEmpty()) {
                    System.out.println("Warning: Line " + string.getLineNumber() + " doesn't have" +
                            " a translation for " + language.getId());
                }

                writer.print("\"" + value + "\"");

                if (i != strings.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            } catch (Exception e) {
                System.out.println("Error on Line " + string.getLineNumber());
                e.printStackTrace();
            }
        }

        // Close the JSON object
        writer.print("}");

        writer.close();
    }
}
