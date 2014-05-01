package stringparser;

/**
 * Author : Julien
 * Date : 04/11/13, 9:02 AM
 * Copyright (c) 2014 Julien Guerinet. All rights reserved.
 */

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StringParser{

    //Config Variables
    public static String url = null;
    public static String englishPath = null;
    public static String frenchPath = null;

    //Platforms
    public static int platform = -1;
    private static final int ANDROID = 0;
    private static final int IOS = 1;
    private static final int WINDOWS = 2;

    //Stuff from the file
    public static final String URL = "URL:";
    public static final String PLATFORM = "Platform:";
    public static final String ANDROID_ENGLISH = "Android English Path:";
    public static final String ANDROID_FRENCH = "Android French Path:";
    public static final String IOS_ENGLISH = "iOS English Path:";
    public static final String IOS_FRENCH = "iOS French Path:";
    public static final String WINDOWS_ENGLISH = "Windows English Path:";
    public static final String WINDOWS_FRENCH = "Windows French Path:";

    //Stuff for Android Strings
    public static final String XML_OPENER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    public static final String RESOURCES_OPENER = "<resources>";
    public static final String RESOURCES_CLOSER = "</resources>";

    public static void main(String[] args) throws IOException {
        //Read from the config file
        BufferedReader configReader = new BufferedReader(new FileReader("config.txt"));
        String line;
        while ((line = configReader.readLine()) != null) {

            //Get the Docs URL
            if(line.startsWith(URL)){
                url = line.replace(URL, "").trim();
            }

            //Get the platform
            else if(line.startsWith(PLATFORM)){
                String platformString = line.replace(PLATFORM, "").trim();
                if(platformString.equalsIgnoreCase("android")){
                    platform = ANDROID;
                }
                else if(platformString.equalsIgnoreCase("ios")){
                    platform = IOS;
                }
                else if(platformString.equalsIgnoreCase("windows")){
                    platform = WINDOWS;
                }
                else{
                    System.out.println("Error: Platform must be either Android, iOS, or Windows.");
                }
            }

            //Get the path for the file.
            else if(platform == ANDROID && line.startsWith(ANDROID_ENGLISH)){
                englishPath = line.replace(ANDROID_ENGLISH, "");
            }
            else if(platform == ANDROID && line.startsWith(ANDROID_FRENCH)){
                frenchPath = line.replace(ANDROID_FRENCH, "");
            }
            else if(platform == IOS && line.startsWith(IOS_ENGLISH)){
                englishPath = line.replace(IOS_ENGLISH, "");
            }
            else if(platform == IOS && line.startsWith(IOS_FRENCH)){
                frenchPath = line.replace(IOS_FRENCH, "");
            }
            else if(platform == WINDOWS && line.startsWith(WINDOWS_ENGLISH)){
                englishPath = line.replace(WINDOWS_ENGLISH, "");
            }
            else if(platform == WINDOWS&& line.startsWith(WINDOWS_FRENCH)){
                frenchPath = line.replace(WINDOWS_FRENCH, "");
            }
        }
        configReader.close();

        //Make sure nothing is null
        if(url == null){
            System.out.println("Error: URL Cannot be null");
            System.exit(0);
        }
        else if(platform == -1){
            System.out.println("Error: You need to input a platform");
            System.exit(0);
        }
        else if(englishPath == null){
            System.out.println("Error: You need to input a path for the english strings");
            System.exit(0);
        }
        else if(frenchPath == null){
            System.out.println("Error: You need to input a path for the french strings");
            System.exit(0);
        }

        //Connect to the URL
        URL link = new URL(url);
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
        httpConnection.setRequestMethod("GET");
        httpConnection.connect();

        //Parse the strings
        List<Strings> strings = new ArrayList<Strings>();
        CsvBeanReader reader = new CsvBeanReader(new InputStreamReader(httpConnection.getInputStream()), CsvPreference.EXCEL_PREFERENCE);

        try {
            final String[] header = reader.getHeader(true);
            final CellProcessor[] processors = getProcessors();

            Strings currentLine;
            while( (currentLine = reader.read(Strings.class, header, processors)) != null){
                strings.add(currentLine);
            }
            if(!strings.isEmpty()){
                processStrings(strings);
            }
        }
        finally {
            reader.close();
        }
    }

    public static CellProcessor[] getProcessors(){
        return new CellProcessor[]{
                null,
                null,
                null,
                null,
        };
    }

    public static void processStrings(List<Strings> strings)throws FileNotFoundException, UnsupportedEncodingException{
        if(platform == ANDROID){
            processAndroidStrings(strings);
        }
        else if(platform == IOS){
            processIOSStrings(strings);
        }
        else{
            //TODO Windows Code
        }
    }

    /** ANDROID STRING PARSING **/
    public static void processAndroidStrings(List<Strings> strings)throws FileNotFoundException, UnsupportedEncodingException{
        //Android English Strings
        PrintWriter androidEnglishWriter = new PrintWriter(englishPath, "UTF-8");
        //Android French Strings
        PrintWriter androidFrenchWriter = new PrintWriter(frenchPath, "UTF-8");

        //Add the XML header for Android files
        androidEnglishWriter.println(XML_OPENER);
        androidFrenchWriter.println(XML_OPENER);

        //Add the resources opening for Android files
        androidEnglishWriter.println(RESOURCES_OPENER);
        androidFrenchWriter.println(RESOURCES_OPENER);

        //Go through the strings
        for(Strings currentStrings : strings){
            //Android strings
            String androidEnglishString = addAndroidEnglishString(currentStrings);
            String androidFrenchString = addAndroidFrenchString(currentStrings);

            //If one is null, there is no value, so do not add it
            if(androidEnglishString != null){
                androidEnglishWriter.println(androidEnglishString);
            }
            if(androidFrenchString != null){
                androidFrenchWriter.println(androidFrenchString);
            }
        }

        //Add the resources closing to android files
        androidEnglishWriter.println(RESOURCES_CLOSER);
        androidFrenchWriter.println(RESOURCES_CLOSER);

        //Close the writers
        androidEnglishWriter.close();
        androidFrenchWriter.close();
    }

    public static String addAndroidEnglishString(Strings strings){
        return addAndroidString(strings.getKey(), strings.getEn());
    }

    public static String addAndroidFrenchString(Strings strings){
        //For headers in the french XML
        if(strings.getKey().equalsIgnoreCase("header")){
            return addAndroidString(strings.getKey(), strings.getEn());
        }
        return addAndroidString(strings.getKey(), strings.getFr());
    }

    public static String addAndroidString(String key, String string){
        //First check if string is empty: if it is, return null
        if(string.isEmpty()){
            return null;
        }

        //Add initial indentation
        String xmlString = "    ";

        //Check if it's a header section
        if(key.trim().equalsIgnoreCase("header")){
            xmlString = "\n" + xmlString + "<!-- " + string + " -->";
        }
        //If not, treat is as a normal string
        else{
            /* Character checks */
            //Unescaped apostrophes
            string = string.replace("\'", "\\" + "\'");

            //Unescaped @ signs
            string = string.replace("@", "\\" + "@");

            if(string.contains("<html>") || string.contains("<HTML>")){
                //Take care of html tags
                string = string.replace("<html>", "<![CDATA[");
                string = string.replace("</html>", "]]>");
                string = string.replace("<HTML>", "<![CDATA[");
                string = string.replace("</HTML>", "]]>");
            }
            else{
                //Ampersands
                if(string.contains("&")){
                    //If it's an icon, do not do anything
                    if(!string.contains("&#x")){
                        string = string.replace("&", "&amp;");
                    }
                }

                //Copyright
                string = string.replace("(c)", "\u00A9");

                //Ellipses
                string = string.replace("...", "&#8230;");
            }

            xmlString = xmlString + "<string name=\"" + key.trim() + "\">" + string + "</string>";
        }
        return xmlString;
    }

    /** IOS STRING PARSING **/
    public static void processIOSStrings(List<Strings> strings)throws FileNotFoundException, UnsupportedEncodingException{
        //IOS English Strings
        PrintWriter iOSEnglishWriter = new PrintWriter(englishPath,"UTF-8");
        //IOS French Strings
        PrintWriter iOSFrenchWriter = new PrintWriter(frenchPath,"UTF-8");

        //Go through the strings
        for(Strings currentStrings : strings){
            //iOS strings
            String iOSEnglishString = addIOSEnglishString(currentStrings);
            String iOSFrenchString = addIOSFrenchString(currentStrings);

            //If one is null, there is no value, so do not add it
            if(iOSEnglishString != null) {
                iOSEnglishWriter.println(iOSEnglishString);
            }
            if(iOSFrenchString != null) {
                iOSFrenchWriter.println(iOSFrenchString);
            }
        }

        iOSEnglishWriter.close();
        iOSFrenchWriter.close();
    }

    public static String addIOSEnglishString(Strings strings){
        return addIOSString(strings.getKey(), strings.getEn());
    }

    public static String addIOSFrenchString(Strings strings){
        //For headers in the french XML
        if(strings.getKey().equalsIgnoreCase("header")){
            return addIOSString(strings.getKey(), strings.getEn());
        }
        return addIOSString(strings.getKey(), strings.getFr());
    }

    public static String addIOSString(String key, String string){
        //First check if string is empty: if it is, return null
        if (string.isEmpty()) {
            return null;
        }

        //Add initial indentation
        String xmlString = "";

        //Replace %s format specifiers with %@
        string = string.replace("%s","%@");
        string = string.replace("$s", "$@");

        //Remove <html> </html>tags
        string = string.replace("<html>", "");
        string = string.replace("</html>", "");
        string = string.replace("<HTML>", "");
        string = string.replace("</HTML>", "");
        string = string.replace("(c)", "\u00A9");

        //Check if it's a header section
        if(key.equalsIgnoreCase("header")){
            xmlString = "\n" + xmlString + "/*  " + string + " */";
        }
        //If not, treat is as a normal string
        else{
            xmlString = "\"" + key + "\" = \"" + string + "\";";
        }
        return xmlString;
    }

    /**WINDOWS STRING PARSING**/
}
