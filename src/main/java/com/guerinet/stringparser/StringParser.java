/*
 * Copyright 2013-2015 Julien Guerinet
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class, executes the main code for parsing the Google Docs file
 * @author Julien Guerinet
 * @version 2.8.0
 * @since 1.0.0
 */
public class StringParser{
    /* FILE STRINGS */
    /**
     * The URL in the file
     */
    private static final String URL = "URL:";
    /**
     * The platform in the file
     */
    private static final String PLATFORM = "Platform:";
    /**
     * Languages in the file
     */
    private static final String LANGUAGE= "Language:";

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
     * The key used for the header in the Strings document
     */
    private static final String HEADER_KEY = "###";

    public static void main(String[] args) throws IOException {
        //Keep a list of all of the languages the Strings are in
        List<Language> languages = new ArrayList<>();
        //The list of language Strings
        List<HeaderString> strings = new ArrayList<>();
        //Url
        String url = null;
        //True if it's for Android, false if it's for iOS
        Boolean android = null;

        //Read from the config file
        BufferedReader configReader = null;
        try{
            configReader = new BufferedReader(new FileReader("../config.txt"));
        }
        catch(FileNotFoundException e){
            try{
                configReader = new BufferedReader(new FileReader("config.txt"));
            }
            catch(FileNotFoundException ex){
                System.out.println("Error: Config file not found");
                System.exit(-1);
            }
        }

        String line;
        while ((line = configReader.readLine()) != null) {
            //Get the URL
            if(line.startsWith(URL)){
                url = line.replace(URL, "").trim();
            }
            //Get the platform
            else if(line.startsWith(PLATFORM)){
                //Remove the header
                String platformString = line.replace(PLATFORM, "").trim();
                //Android
                if(platformString.equalsIgnoreCase("android")){
                    android = true;
                }
                //iOS
                else if(platformString.equalsIgnoreCase("ios")){
                    android = false;
                }
                //Not recognized
                else{
                    System.out.println("Error: Platform must be either Android or iOS.");
                    System.exit(-1);
                }
            }
            //Get the languages
            else if(line.startsWith(LANGUAGE)){
                //Remove the header and separate the language Id from the path
                String languageString= line.replace(LANGUAGE, "").trim();
                String[] languageInfo = languageString.split(", ");

                if(languageInfo.length != 2){
                    System.out.println("Error: The following format has too few or too many " +
                        "arguments for a language: " + languageString);
                    System.exit(-1);
                }

                //Save it as a new language in the list of languages
                languages.add(new Language(languageInfo[0], languageInfo[1]));
            }
        }
        configReader.close();

        //Make sure nothing is null
        if(url == null){
            System.out.println("Error: URL Cannot be null");
            System.exit(-1);
        }
        else if(android == null){
            System.out.println("Error: You need to input a platform");
            System.exit(-1);
        }
        else if(languages.isEmpty()){
            System.out.println("Error: You need to add at least one language");
            System.exit(-1);
        }

        //Connect to the URL
        System.out.println("Connecting to " + url);
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();

        Response response;
        try{
            response = new OkHttpClient().newCall(request).execute();
        } catch(IOException e){
            //Catch the exception here to be able to continue a build even if we are not connected
            System.out.println("IOException while connecting to the URL");
            System.out.println("Error Message: " + e.getMessage());
            return;
        }

        int responseCode = response.code();
        System.out.println("Response Code: " + responseCode);

        if(responseCode == 200){
            //Set up the CSV reader
            CsvListReader reader = new CsvListReader(new InputStreamReader(
                    response.body().byteStream(), "UTF-8"), CsvPreference.EXCEL_PREFERENCE);

            //Get the header
            final String[] header = reader.getHeader(true);

            //First column will be key, so ignore it
            for(int i = 1; i < header.length; i++){
                String string = header[i];

                //Check if the string matches any of the languages parsed
                for(Language language : languages){
                    if(string.equals(language.getId())){
                        //If we find a match, set the column index for this language
                        language.setColumnIndex(i);
                        break;
                    }
                }
            }

            //Make sure that all languages have an index
            for(Language language : languages){
                if(language.getColumnIndex() == -1){
                    System.out.println("Error: " + language.getId() +
                            " does not have any translations.");
                    System.exit(-1);
                }
            }

            //Make a CellProcessor with the right length
            final CellProcessor[] processors = new CellProcessor[header.length];

            //Go through each line of the CSV document into a list of objects.
            List<Object> currentLine;
            //The current line number (start at 2 since 1 is the header)
            int lineNumber = 2;
            while((currentLine = reader.read(processors)) != null){
                //Get the key from the current line
                String key = (String)currentLine.get(0);

                //Check if there's a key
                if(key == null || key.trim().isEmpty()){
                    System.out.println("Warning: Line " + lineNumber + " does not have " +
                            "a kay and will not be parsed");

                    //Increment the line number
                    lineNumber++;

                    //Move on to the new String
                    continue;
                }

                //Check if this is a header
                if(key.trim().startsWith(HEADER_KEY)){
                    strings.add(new HeaderString(key.replace("###", "").trim(), lineNumber));

                    //Increment the line number and continue
                    lineNumber++;
                    continue;
                }

                //Add a new language String
                LanguageString languageString = new LanguageString(key.trim(), lineNumber);

                //Go through the languages, add each translation
                boolean allNull = true;
                for(Language language : languages){
                    languageString.addTranslation(language.getId(),
                            (String)currentLine.get(language.getColumnIndex()));

                    //If at least one language is not null, then they are not all null
                    if(languageString.getString(language.getId()) != null){
                        allNull = false;
                    }
                }

                //Check if all of the values are null
                if(allNull){
                    //Show a warning message
                    System.out.println("Warning: Line " + lineNumber + " has no " +
                            "translations so it will not be parsed.");
                }
                else{
                    strings.add(languageString);
                }

                //Increment the line number
                lineNumber++;
            }

            //Close the CSV reader
            reader.close();

            //Check if there are any errors with the keys
            for (int i = 0; i < strings.size(); i++){
                HeaderString string1 = strings.get(i);

                //Skip headers for the checks
                if(!(string1 instanceof LanguageString)){
                    continue;
                }

                //Check if there are any spaces in the keys
                if(string1.getKey().contains(" ")){
                    System.out.println("Error: Line " + string1.getLineNumber() +
                            " contains a space in its key.");
                    System.exit(-1);
                }

                //Check if there are any duplicates
                for(int j = i + 1; j < strings.size(); j++){
                    HeaderString string2 = strings.get(j);

                    //If the keys are the same and it's not a header, show an error and stop
                    if(string1.getKey().equals(string2.getKey())){
                        System.out.println("Error: Lines " + string1.getLineNumber() + " and " +
                                string2.getLineNumber() + " have the same key.");
                        System.exit(-1);
                    }
                }
            }

            //Go through each language, and write the file
            PrintWriter writer;
            for(Language language : languages){
                //Set up the writer for the given language, enforcing UTF-8
                writer = new PrintWriter(language.getPath(), "UTF-8");

                if(android){
                    processAndroidStrings(writer, language, strings);
                }
                else{
                    processIOSStrings(writer, language, strings);
                }

                System.out.println("Wrote " + language.getId() + " to file: " + language.getPath());

                writer.close();
            }

            //Exit message
            System.out.println("Strings parsing complete");
        }
        else{
            System.out.println("Error: Response Code not 200");
            System.out.println("Response Message: " + response.message());
        }
    }

    /* HELPERS */

    /**
     * Processes the String by doing some checks and preparing some special characters
     *
     * @param string   The LanguageString object
     * @param language The language to parse the String for
     * @return The formatted String for the given language and platform
     */
    private static String processString(HeaderString string, Language language){
        String value;
        //Check if we are parsing a header, use the key for the value
        if(!(string instanceof LanguageString)){
            value = string.getKey();
        }
        else{
            value = ((LanguageString) string).getString(language.getId());
        }

        //Check if value is or null empty: if it is, return null
        if(value == null || value.isEmpty()){
            return null;
        }

        //Process the value with the general methods first:

        //Unescaped quotations
        value = value.replace("\"", "\\" + "\"");

        //Copyright
        value = value.replace("(c)", "\u00A9");

        //New Lines
        value = value.replace("\n", "");

        return value;
    }

    /**
     * Processes the list of parsed Strings into the Android XML document
     *
     * @param writer   The writer to use to write to the file
     * @param language The language to parse the Strings for
     * @param strings  The list of Strings
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    private static void processAndroidStrings(PrintWriter writer, Language language,
                                              List<HeaderString> strings)
            throws FileNotFoundException, UnsupportedEncodingException{
        //Add the header
        writer.println(XML_OPENER);
        writer.println(RESOURCES_OPENER);

        //Go through the strings
        for(HeaderString currentString : strings){
            try{
                //Process the String
                String string = processString(currentString, language);

                //If the String is null, continue
                if(string == null){
                    continue;
                }

                //Add initial indentation
                String xmlString = "    ";

                //Check if it's a header section
                if(!(currentString instanceof LanguageString)){
                    //Leave a space before it, add the header as a comment
                    xmlString = "\n" + xmlString + "<!-- " + string + " -->";
                }
                //If not, treat is as a normal string
                else{
                    /* Character checks */
                    //Ampersands
                    string = string.replace("&", "&amp;");

                    //Apostrophes
                    string = string.replace("'", "\\'");

                    //Unescaped @ signs
                    string = string.replace("@", "\\" + "@");

                    //Ellipses
                    string = string.replace("...", "&#8230;");

                    //Greater than
                    string = string.replace(">", "&gt;");

                    //Less than
                    string = string.replace("<", "&lt;");

                    //HTML content
                    string = string.replace("<html>", "<![CDATA[");
                    string = string.replace("</html>", "]]>");
                    string = string.replace("<HTML>", "<![CDATA[");
                    string = string.replace("</HTML>", "]]>");

                    //Add the XML tag
                    xmlString = xmlString + "<string name=\"" + currentString.getKey() + "\">" +
                            string + "</string>";
                }

                //Write the String
                writer.println(xmlString);
            }
            catch (Exception e){
                System.out.println("Error on Line " + currentString.getLineNumber());
                e.printStackTrace();
            }
        }

        //Add the resources closing to android files
        writer.println(RESOURCES_CLOSER);
    }

    /**
     * Processes the list of parsed Strings into the iOS Strings document
     *
     * @param writer   The writer to use to write to file
     * @param language The language to parse the Strings for
     * @param strings  The list of Strings
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public static void processIOSStrings(PrintWriter writer, Language language,
                                         List<HeaderString> strings)
            throws FileNotFoundException, UnsupportedEncodingException{
        //Go through the strings
        for(HeaderString currentString : strings){
            try{
                //Get the processed String
                String string = processString(currentString, language);

                //If the String is null, there is no value so do not add it
                if(string == null){
                    continue;
                }

                //Replace %s format specifiers with %@
                string = string.replace("%s", "%@");
                string = string.replace("$s", "$@");

                //Remove <html> </html>tags
                string = string.replace("<html>", "");
                string = string.replace("</html>", "");
                string = string.replace("<HTML>", "");
                string = string.replace("</HTML>", "");

                //Check if it's a header section
                if(!(currentString instanceof LanguageString)){
                    string = "\n" + "/*  " + string + " */";
                }
                //If not, treat is as a normal string
                else{
                    string = "\"" + currentString.getKey() + "\" = \"" + string + "\";";
                }

                writer.println(string);
            }
            catch (Exception e){
                System.out.println("Error on Line " + currentString.getLineNumber());
                e.printStackTrace();
            }
        }
    }
}
