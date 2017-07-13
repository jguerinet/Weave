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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Main class, executes the main code for parsing the Google Docs file
 * @author Julien Guerinet
 * @since 1.0.0
 */
public class StringParser {

    /* PLATFORM CONSTANTS */

    private static final String ANDROID = "Android";
    private static final String IOS = "iOS";
    private static final String WEB = "Web";

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

    /**
     * Key used for the header in the Strings document
     */
    private static final String HEADER_KEY = "###";

    /* INSTANCE VARIABLES */

    /**
     * List of languages we are writing to
     */
    protected List<Language> languages;

    /**
     * List of Urls we are downloading from
     */
    protected Map<String, String> urls;

    /**
     * Platform we are downloading for
     */
    protected String platform;

    /**
     * List of Strings to write
     */
    protected List<BaseString> strings;

    public static void main(String[] args) throws IOException {
        new StringParser().run();
    }

    /**
     * Runs the {@link StringParser}
     */
    public void run() {
        try {
            setup();
            readFromConfigFile();
            verifyConfigInfo();
            downloadAllStrings();
            verifyKeys();
            writeStrings();
            System.out.println("Strings parsing complete");
        } catch (IOException e) {
            System.out.println("Error downloading Strings: ");
            e.printStackTrace();
        }
    }

    /**
     * Sets up the variables
     */
    protected void setup() {
        // List of all of the languages the Strings are in
        languages = new ArrayList<>();

        // List of Urls to get the Strings from
        urls = new HashMap<>();

        // Platform this is for (null if not chosen)
        platform = null;

        // List of Strings to write
        strings = new ArrayList<>();
    }

    /**
     * Reads and parses the various pieces of info from the config file
     *
     * @throws IOException Thrown if there was an error opening or reading the config file
     */
    protected void readFromConfigFile() throws IOException {
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
                // Get the Url: remove the header and separate the file name from the Url
                String urlString = line.replace(URL, "").trim();
                String[] urlInfo = urlString.split(",");

                if (urlInfo.length < 2) {
                    System.out.println("Error: The following format has too few " +
                            "arguments for a Url: " + urlString);
                    System.exit(-1);
                }

                if (!urlInfo[1].trim().isEmpty()) {
                    // Save it as a new Url in the Url map
                    urls.put(urlInfo[0].trim(), urlInfo[1].trim());
                }
            } else if (line.startsWith(PLATFORM)) {
                // Get the platform: Remove the header
                String platformString = line.replace(PLATFORM, "").trim();
                if (platformString.equalsIgnoreCase(ANDROID)) {
                    platform = ANDROID;
                } else if (platformString.equalsIgnoreCase(IOS)) {
                    platform = IOS;
                } else if (platformString.equalsIgnoreCase(WEB)) {
                    platform = WEB;
                } else {
                    // Not recognized
                    System.out.println("Error: Platform must be either Android, iOS, or Web.");
                    System.exit(-1);
                }
            } else if (line.startsWith(LANGUAGE)) {
                // Get the languages: remove the header and separate the language Id from the path
                String languageString = line.replace(LANGUAGE, "").trim();
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
    }

    /**
     * Verifies that all of the config info is present
     */
    protected void verifyConfigInfo() {
        // Make sure everything is set
        if (urls.isEmpty()) {
            System.out.println("Error: There must be at least one non-null Url");
            System.exit(-1);
        } else if (platform == null) {
            System.out.println("Error: You need to input a platform");
            System.exit(-1);
        } else if (languages.isEmpty()) {
            System.out.println("Error: You need to add at least one language");
            System.exit(-1);
        }

        // Make sure that we have a path per language
        for (Language language : languages) {
            if (language.getPath() == null) {
                System.out.println("Error: Languages need a file path for Android and iOS");
                System.exit(-1);
            }
        }
    }

    /**
     * Downloads all of the Strings from all of the Urls
     *
     * @throws IOException Thrown if there is any error downloading the Strings
     */
    protected void downloadAllStrings() throws IOException {
        for (String urlKey : urls.keySet()) {
            // Go through the Urls and download all of the Strings
            List<BaseString> urlStrings = downloadStrings(urlKey, urls.get(urlKey));

            if (urlStrings == null) {
                // Don't continue if there's an error downloading the Strings
                return;
            }

            strings.addAll(urlStrings);
        }
    }

    /**
     * Connects to the given Url and downloads the Strings in the right format
     *
     * @param url       Url to connect to
     * @return List of Strings that were downloaded, null if there was an error
     * @throws IOException Thrown if there was any errors downloading the Strings
     */
    protected List<BaseString> downloadStrings(String urlName, String url) throws IOException {
        // Connect to the URL
        System.out.println("Connecting to " + url);
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();

        Response response;
        try {
            response = new OkHttpClient().setCache(null).newCall(request).execute();
        } catch (IOException e) {
            // Catch the exception here to be able to continue a build even if we are not connected
            System.out.println("IOException while connecting to the URL");
            System.out.println("Error Message: " + e.getMessage());
            return null;
        }

        int responseCode = response.code();
        System.out.println("Response Code: " + responseCode);

        if (responseCode != 200) {
            System.out.println("Error: Response Message: " + response.message());
            return null;
        }

        // Set up the CSV reader
        CsvListReader reader = new CsvListReader(new InputStreamReader(response.body().byteStream(),
                "UTF-8"), CsvPreference.EXCEL_PREFERENCE);

        // Keep track of which columns hold the keys and the platform
        int keyColumn = -1;
        int platformColumn = -1;

        // Get the header
        final String[] header = reader.getHeader(true);

        for (int i = 0; i < header.length; i ++) {
            String string = header[i];

            if (string == null) {
                // Disregard null headers
                continue;
            }

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
                System.out.println("Error: " + language.getId() + " in " + urlName +
                        " does not have any translations.");
                System.exit(-1);
            }
        }

        // Create the list of Strings
        List<BaseString> strings = new ArrayList<>();

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
                strings.add(new BaseString(key.replace("###", "").trim(), urlName, lineNumber));

                // Increment the line number and continue
                lineNumber ++;
                continue;
            }

            // Add a new language String
            LanguageString languageString = new LanguageString(key, urlName, lineNumber);

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
                System.out.println("Warning: Line " + lineNumber + " from " + urlName +
                        " has no translations so it will not be parsed.");
            } else {
                if (oneNull) {
                    System.out.println("Warning: Line " + lineNumber + " from " + urlName +
                            " is missing at least one translation");
                }
                strings.add(languageString);
            }

            // Increment the line number
            lineNumber++;
        }

        // Close the CSV reader
        reader.close();

        return strings;
    }

    /**
     * Verifies that the keys are valid
     */
    protected void verifyKeys() {
        // Define the key checker pattern to make sure no illegal characters exist within the keys
        Pattern keyChecker = Pattern.compile("[^A-Za-z0-9_]");

        // Check if there are any errors with the keys
        for (int i = 0; i < strings.size(); i ++) {
            BaseString string1 = strings.get(i);

            // Skip headers for the checks
            if (!(string1 instanceof LanguageString)) {
                continue;
            }

            // Check if there are any spaces in the keys
            if (string1.getKey().contains(" ")) {
                System.out.println("Error: " + getLog(string1) + " contains a space in its key.");
                System.exit(-1);
            }

            if (keyChecker.matcher(string1.getKey()).find()) {
                System.out.println("Error: " + getLog(string1) +
                        " contains some illegal characters.");
                System.exit(-1);
            }

            // Check if there are any duplicates
            for (int j = i + 1; j < strings.size(); j ++) {
                BaseString string2 = strings.get(j);

                // If the keys are the same and it's not a header, show an error and stop
                if (string1.getKey().equals(string2.getKey())) {
                    System.out.println("Error: " + getLog(string1) + " and " + getLog(string2) +
                            " have the same key.");
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * Writes all of the Strings for the different languages
     *
     * @throws IOException Thrown if any error happens
     */
    protected void writeStrings() throws IOException {
        // Go through each language, and write the file
        PrintWriter writer;
        for (Language language : languages) {
            // Set up the writer for the given language, enforcing UTF-8
            writer = new PrintWriter(language.getPath(), "UTF-8");

            writeLanguageStrings(writer, language);

            System.out.println("Wrote " + language.getId() + " to file: " + language.getPath());

            writer.flush();
            writer.close();
        }
    }

    /**
     * @param string String we want to log
     * @return Log message to use to designate the given String
     */
    protected String getLog(BaseString string) {
        return "Line " + string.getLineNumber() + " from " + string.getUrl();
    }

    /**
     * Processes the Strings and writes them to a given file
     *
     * @param writer   Writer to use to write the Strings to the given file
     * @param language Language we are writing for
     * @throws FileNotFoundException Thrown if the file does not exist
     */
    protected void writeLanguageStrings(PrintWriter writer, Language language)
            throws FileNotFoundException {

        // Header
        write(writer, getHeader(platform));

        for (int i = 0; i < strings.size(); i ++) {
            BaseString currentString = strings.get(i);

            try {
                if (!(currentString instanceof LanguageString)) {
                    // If we are parsing a header, right the value as a comment
                    write(writer, getComment(platform, currentString.getKey()));
                    continue;
                }
                // Normal String
                LanguageString languageString = (LanguageString) currentString;

                // Check that it's for the right platform
                if (!languageString.isForPlatform(platform)) {
                    continue;
                }

                String string = languageString.getString(language.getId());

                // Check if value is or null empty: if it is, continue
                if (string == null) {
                    string = "";
                }

                if (string.isEmpty() && !platform.equalsIgnoreCase(WEB)) {
                    // Skip over the empty values unless we're on Web
                    continue;
                }

                // Unescaped quotations
                string = string.replace("\"", "\\" + "\"");

                // Copyright
                string = string.replace("(c)", "\u00A9");

                // New Lines
                string = string.replace("\n", "");

                write(writer, getProcessedString(platform, languageString.getKey(), string,
                        i == strings.size() - 1));
            } catch (Exception e) {
                System.out.println("Error on " + getLog(currentString));
                e.printStackTrace();
            }
        }

        // Header
        write(writer, getFooter(platform));
    }

    /**
     * @param writer Writer to use to write the String
     * @param string String to write, null if none
     */
    protected void write(PrintWriter writer, String string) {
        if (string != null) {
            writer.println(string);
        }
    }

    /**
     * @param platform Current platform
     * @return String to use as the header for the Strings file, null if none
     */
    protected String getHeader(String platform) {
        if (platform.equalsIgnoreCase(ANDROID)) {
            return "<?xml version=\"1.0\" encoding=\"utf-8\"?> \n <resources>";
        } else if (platform.equalsIgnoreCase(IOS)) {
            return null;
        } else {
            return "{";
        }
    }

    /**
     * @param platform Current platform
     * @param comment  Comment String
     * @return String to include in the Strings file as a comment, null if none
     */
    protected String getComment(String platform, String comment) {
        if (platform.equalsIgnoreCase(ANDROID)) {
            return "\n    <!-- " + comment + " -->";
        } else if (platform.equalsIgnoreCase(IOS)) {
            return "\n/* " + comment + " */";
        } else {
            return null;
        }
    }

    /**
     * @param platform   Current platform
     * @param key        String key
     * @param string     String to process
     * @param lastString True if this is the last String, false otherwise
     * @return String to include in the Strings file for the given platform, null if none
     */
    protected String getProcessedString(String platform, String key, String string,
            boolean lastString) {
        if (platform.equalsIgnoreCase(ANDROID)) {
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

            // Add the XML tag
            return "    <string name=\"" + key + "\">" + string + "</string>";
        } else if (platform.equalsIgnoreCase(IOS)) {
            // Replace %s format specifiers with %@
            string = string.replace("%s", "%@");
            string = string.replace("$s", "$@");

            // Remove <html> </html>tags
            string = string.replace("<html>", "");
            string = string.replace("</html>", "");
            string = string.replace("<HTML>", "");
            string = string.replace("</HTML>", "");

            return "\"" + key + "\" = \"" + string + "\";";
        } else {
            // Remove <html> </html>tags
            string = string.replace("<html>", "");
            string = string.replace("</html>", "");
            string = string.replace("<HTML>", "");
            string = string.replace("</HTML>", "");

            return "    \"" + key + "\": \"" + string + "\"" + (lastString ? "" : ",");
        }
    }

    /**
     * @param platform Current platform
     * @return String to include as the footer of the Strings file, null if none
     */
    protected String getFooter(String platform) {
        if (platform.equalsIgnoreCase(ANDROID)) {
            return "</resources>";
        } else if (platform.equalsIgnoreCase(IOS)) {
            return null;
        } else {
            return "}";
        }
    }
}
