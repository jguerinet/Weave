/*
 * Copyright (c) 2015 Julien Guerinet. All rights reserved.
 */

package stringparser;

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class, executes the main code for parsing the Google Docs file
 * @author Julien Guerinet
 * @version 1.0
 * @since 1.0
 */
public class StringParser{
    /* PLATFORMS */
    /**
     * The platform chosen
     */
    private static int platform = -1;
    /**
     * Android platform
     */
    private static final int ANDROID = 0;
    /**
     * iOS platform
     */
    private static final int IOS = 1;

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
     * The English Id, used to get the English Strings for the header
     */
    private static final String ENGLISH_ID = "en";
    /**
     * The key used for the header in the Strings document
     */
    private static final String HEADER_KEY = "header";

    public static void main(String[] args) throws IOException {
        //Keep a list of all of the languages the Strings are in
        List<Language> languages = new ArrayList<Language>();
        //The list of language Strings
        List<LanguageString> strings = new ArrayList<LanguageString>();
        //Url
        String url = null;

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
                    platform = ANDROID;
                }
                //iOS
                else if(platformString.equalsIgnoreCase("ios")){
                    platform = IOS;
                }
                //Not recognized
                else{
                    System.out.println("Error: Platform must be either Android, iOS, or Windows.");
                }
            }
            //Get the languages
            else if(line.startsWith(LANGUAGE)){
                //Remove the header and separate the language Id from the path
                String[] languageInfo = line.replace(LANGUAGE, "").trim().split(", ");

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
        else if(platform == -1){
            System.out.println("Error: You need to input a platform");
            System.exit(-1);
        }
        else if(languages.isEmpty()){
            System.out.println("Error: You need to add at least one language");
            System.exit(-1);
        }

        //Connect to the URL
        URL link = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) link.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        //Set up the CSV reader
        CsvListReader reader = new CsvListReader(new InputStreamReader(connection.getInputStream()),
                CsvPreference.EXCEL_PREFERENCE);

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
        while((currentLine = reader.read(processors)) != null){
            //Add a new language String
            LanguageString languageString = new LanguageString((String)currentLine.get(0));

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
                System.out.println("Warning: Line " + (strings.size() + 2) + " has no " +
                    "translations so it will not be parsed.");
            }
            else{
                strings.add(languageString);
            }
        }

        //Close the CSV reader
        reader.close();

        //Check if there are any errors with the keys
        for (int i = 0; i < strings.size(); i++){
            LanguageString string1 = strings.get(i);

            //Check if there are any spaces in the keys
            if(string1.getKey().trim().contains(" ")){
                System.out.println("Error: Line " + getLineNumber(string1, strings) + " contains " +
                                "a space in its key.");
                System.exit(-1);
            }

            //Check if there are any duplicated
            for(int j = i + 1; j < strings.size(); j++){
                LanguageString string2 = strings.get(j);

                //If the keys are the same and it's not a header, show an error and stop
                if(!string1.getKey().equalsIgnoreCase(HEADER_KEY) &&
                        string1.getKey().equals(string2.getKey())){
                    System.out.println("Error: Lines " + getLineNumber(string1, strings) + " and " +
                            getLineNumber(string2, strings) + " have the same key.");
                    System.exit(-1);
                }
            }
        }

        //Go through each language, and write the file
        for(Language language : languages){
            if(platform == ANDROID){
                processAndroidStrings(language, strings);
            }
            else{
                processIOSStrings(language, strings);
            }
        }

        //Exit message
        System.out.println("Done parsing Strings");
    }

    /* HELPERS */

    /**
     * Get the line number of a given String for any warnings or errors shown to the user
     *
     * @param string The String
     * @return The line number of the String
     */
    private static int getLineNumber(LanguageString string, List<LanguageString> strings){
        //+2 to account for the header and the fact that Google Drive starts numbering at 1
        return strings.indexOf(string) + 2;
    }

    /**
     * Processes a given String with the common changes to make between the platforms
     *
     * @param string The String to process
     */
    private static String processString(String string){
        //Unescaped quotations
        string = string.replace("\"", "\\" + "\"");

        //Copyright
        string = string.replace("(c)", "\u00A9");

        return string;
    }

    /**
     * Add a language String
     *
     * @param platform The platform this is for
     * @param string   The LanguageString object
     * @param language The language to parse the String for
     * @return The formatted String for the given language and platform
     */
    private static String getLanguageString(int platform, LanguageString string, Language language){
        String key = string.getKey();
        String value;
        //Check if we are parsing a header, use the English translation for the value
        if(string.getKey().equalsIgnoreCase(HEADER_KEY)){
            value = string.getString(ENGLISH_ID);
        }
        else{
            value = string.getString(language.getId());
        }

        //Check if value is or null empty: if it is, return null
        if(value == null || value.isEmpty()){
            return null;
        }

        //Process the value with the general methods first
        value = processString(value);

        //Use the right platform method
        if(platform == ANDROID){
            return getAndroidString(key, value);
        }
        return getIOSString(key, value);
    }

    /* ANDROID STRING PARSING */

    /**
     * Get the formatted String for the Android Strings document
     *
     * @param key    The String key
     * @param string The String
     * @return The formatted String
     */
    private static String getAndroidString(String key, String string){
        //Add initial indentation
        String xmlString = "    ";

        //Check if it's a header section
        if(key.trim().equalsIgnoreCase(HEADER_KEY)){
            //Leave a space before it, add the header as a comment
            xmlString = "\n" + xmlString + "<!-- " + string + " -->";
        }
        //If not, treat is as a normal string
        else{
            /* Character checks */
            //Unescaped apostrophes
            string = string.replace("\'", "\\" + "\'");

            //Unescaped @ signs
            string = string.replace("@", "\\" + "@");

            //Ampersands
            if(string.contains("&")){
                string = string.replace("&", "&amp;");
            }

            //Ellipses
            string = string.replace("...", "&#8230;");

            //HTML content
            if(string.contains("<html>") || string.contains("<HTML>")){
                string = string.replace("<html>", "<![CDATA[");
                string = string.replace("</html>", "]]>");
                string = string.replace("<HTML>", "<![CDATA[");
                string = string.replace("</HTML>", "]]>");
            }

            //Add the XML tag
            xmlString = xmlString + "<string name=\"" + key.trim() + "\">" + string + "</string>";
        }

        return xmlString;
    }

    /**
     * Processes the list of parsed Strings into the Android XML document
     *
     * @param language The language to parse the Strings for
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    private static void processAndroidStrings(Language language, List<LanguageString> strings)
            throws FileNotFoundException, UnsupportedEncodingException{

        //Set up the writer for the given language
        PrintWriter writer = new PrintWriter(language.getPath());

        //Add the header
        writer.println(XML_OPENER);
        writer.println(RESOURCES_OPENER);

        //Go through the strings
        for(LanguageString currentString : strings){
            try{
                //If there is no key, we cannot parse it so show a warning and move on
                if(currentString.getKey() == null || currentString.getKey().trim().isEmpty()){
                    System.out.println("Warning: Line " + getLineNumber(currentString, strings) +
                            " has no key, and therefore cannot be parsed");
                    continue;
                }

                //Get the String
                String androidString = getLanguageString(ANDROID, currentString, language);

                //If it is null, there is no value so don't add it
                if(androidString != null){
                    writer.println(androidString);
                }
            }
            catch (Exception e){
                System.out.println("Error on Line " + getLineNumber(currentString, strings));
                e.printStackTrace();
            }
        }

        //Add the resources closing to android files
        writer.println(RESOURCES_CLOSER);

        //Close the writer
        writer.close();
    }

    /* IOS STRING PARSING */

    /**
     * Get the formatted String for the iOS Strings document
     *
     * @param key    The String key
     * @param string The String
     * @return The formatted String
     */
    private static String getIOSString(String key, String string){
        //Replace %s format specifiers with %@
        string = string.replace("%s","%@");
        string = string.replace("$s", "$@");

        //Remove <html> </html>tags
        string = string.replace("<html>", "");
        string = string.replace("</html>", "");
        string = string.replace("<HTML>", "");
        string = string.replace("</HTML>", "");

        //Check if it's a header section
        if(key.equalsIgnoreCase(HEADER_KEY)){
            return "\n" + "/*  " + string + " */";
        }
        //If not, treat is as a normal string
        else{
            return "\"" + key + "\" = \"" + string + "\";";
        }
    }

    /**
     * Processes the list of parsed Strings into the iOS Strings document
     *
     * @param language The language to parse the Strings for
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public static void processIOSStrings(Language language, List<LanguageString> strings)
            throws FileNotFoundException, UnsupportedEncodingException{

        //Set up the writer for the given language
        PrintWriter writer = new PrintWriter(language.getPath());

        //Go through the strings
        for(LanguageString currentString : strings){
            try{
                //If there is no Id, we cannot parse it so show a warning and continue
                if(currentString.getKey() == null){
                    System.out.println("Warning: Line " + getLineNumber(currentString, strings) +
                            " has no Id, and therefore cannot be parsed");
                    continue;
                }

                //Get the iOS String
                String iOSString = getLanguageString(IOS, currentString, language);

                //If the String is null, there is no value so do not add it
                if(iOSString != null) {
                    writer.println(iOSString);
                }
            }
            catch (Exception e){
                System.out.println("Error on Line " + getLineNumber(currentString, strings));
                e.printStackTrace();
            }
        }

        //Close the writer
        writer.close();
    }
}
