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
    private static String url = null;
    private static String englishPath = null;
    private static String frenchPath = null;

    //Platforms
    private static int platform = -1;
    private static final int ANDROID = 0;
    private static final int IOS = 1;
    private static final int WINDOWS = 2;

    //Stuff from the file
    private static final String URL = "URL:";
    private static final String PLATFORM = "Platform:";
    private static final String ANDROID_ENGLISH = "Android English Path:";
    private static final String ANDROID_FRENCH = "Android French Path:";
    private static final String IOS_ENGLISH = "iOS English Path:";
    private static final String IOS_FRENCH = "iOS French Path:";
    private static final String WINDOWS_ENGLISH = "Windows English Path:";
    private static final String WINDOWS_FRENCH = "Windows French Path:";

    //Writers
    private static PrintWriter englishWriter;
    private static PrintWriter frenchWriter;

    //To keep track of which line number you are at
    private static int lineNumber;

    public static void main(String[] args) throws IOException {
        //Read from the config file
        BufferedReader configReader;
        try{
            configReader = new BufferedReader(new FileReader("../config.txt"));
        }
        catch(FileNotFoundException e){
            configReader = new BufferedReader(new FileReader("config.txt"));
        }

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
                englishPath = line.replace(ANDROID_ENGLISH, "").trim();
            }
            else if(platform == ANDROID && line.startsWith(ANDROID_FRENCH)){
                frenchPath = line.replace(ANDROID_FRENCH, "").trim();
            }
            else if(platform == IOS && line.startsWith(IOS_ENGLISH)){
                englishPath = line.replace(IOS_ENGLISH, "").trim();
            }
            else if(platform == IOS && line.startsWith(IOS_FRENCH)){
                frenchPath = line.replace(IOS_FRENCH, "").trim();
            }
            else if(platform == WINDOWS && line.startsWith(WINDOWS_ENGLISH)){
                englishPath = line.replace(WINDOWS_ENGLISH, "").trim();
            }
            else if(platform == WINDOWS&& line.startsWith(WINDOWS_FRENCH)){
                frenchPath = line.replace(WINDOWS_FRENCH, "").trim();
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

        //Set up the writers
        englishWriter = new PrintWriter(englishPath, "UTF-8");
        frenchWriter = new PrintWriter(frenchPath, "UTF-8");

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
            processWindowsStrings(strings);
        }
    }

    /** ANDROID STRING PARSING **/
    //Stuff for Android Strings
    public static final String XML_OPENER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    public static final String RESOURCES_OPENER = "<resources>";
    public static final String RESOURCES_CLOSER = "</resources>";

    public static void processAndroidStrings(List<Strings> strings)throws FileNotFoundException, UnsupportedEncodingException{
        //Add the XML header for Android files
        englishWriter.println(XML_OPENER);
        frenchWriter.println(XML_OPENER);

        //Add the resources opening for Android files
        englishWriter.println(RESOURCES_OPENER);
        frenchWriter.println(RESOURCES_OPENER);

        //Go through the strings
        for(Strings currentStrings : strings){
            try{
                lineNumber = strings.indexOf(currentStrings) + 2;

                //If there is no ID, we cannot parse it, so show a warning
                if(currentStrings.getKey() == null){
                    System.out.println("Warning! Line " + lineNumber + " has no ID, and therefore cannot be parsed");
                    continue;
                }

                //Android strings
                String androidEnglishString = addAndroidEnglishString(currentStrings);
                String androidFrenchString = addAndroidFrenchString(currentStrings);

                //If one is null, there is no value, so do not add it
                if(androidEnglishString != null){
                    englishWriter.println(androidEnglishString);
                }
                if(androidFrenchString != null){
                    frenchWriter.println(androidFrenchString);
                }
            }
            catch (Exception e){
                System.out.println("Error on Line " + lineNumber);
                e.printStackTrace();
            }
        }

        //Add the resources closing to android files
        englishWriter.println(RESOURCES_CLOSER);
        frenchWriter.println(RESOURCES_CLOSER);

        //Close the writers
        englishWriter.close();
        frenchWriter.close();
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

            //Unescaped quotations
            string = string.replace("\"", "\\" + "\"");

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
        //Go through the strings
        for(Strings currentStrings : strings){
            try{
                lineNumber = strings.indexOf(currentStrings) + 2;

                //If there is no ID, we cannot parse it, so show a warning
                if(currentStrings.getKey() == null){
                    System.out.println("Warning! Line " + lineNumber + " has no ID, and therefore cannot be parsed");
                    continue;
                }

                //iOS strings
                String iOSEnglishString = addIOSEnglishString(currentStrings);
                String iOSFrenchString = addIOSFrenchString(currentStrings);

                //If one is null, there is no value, so do not add it
                if(iOSEnglishString != null) {
                    englishWriter.println(iOSEnglishString);
                }
                if(iOSFrenchString != null) {
                    frenchWriter.println(iOSFrenchString);
                }
            }
            catch (Exception e){
                System.out.println("Error on Line " + lineNumber);
                e.printStackTrace();
            }
        }

        englishWriter.close();
        frenchWriter.close();
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
    public static final String WINDOWS_FILE_OPENER = "<root>\n" +
            "  <!-- \n" +
            "    Microsoft ResX Schema \n" +
            "    \n" +
            "    Version 2.0\n" +
            "    \n" +
            "    The primary goals of this format is to allow a simple XML format \n" +
            "    that is mostly human readable. The generation and parsing of the \n" +
            "    various data types are done through the TypeConverter classes \n" +
            "    associated with the data types.\n" +
            "    \n" +
            "    Example:\n" +
            "    \n" +
            "    ... ado.net/XML headers & schema ...\n" +
            "    <resheader name=\"resmimetype\">text/microsoft-resx</resheader>\n" +
            "    <resheader name=\"version\">2.0</resheader>\n" +
            "    <resheader name=\"reader\">System.Resources.ResXResourceReader, System.Windows.Forms, ...</resheader>\n" +
            "    <resheader name=\"writer\">System.Resources.ResXResourceWriter, System.Windows.Forms, ...</resheader>\n" +
            "    <data name=\"Name1\"><value>this is my long string</value><comment>this is a comment</comment></data>\n" +
            "    <data name=\"Color1\" type=\"System.Drawing.Color, System.Drawing\">Blue</data>\n" +
            "    <data name=\"Bitmap1\" mimetype=\"application/x-microsoft.net.object.binary.base64\">\n" +
            "        <value>[base64 mime encoded serialized .NET Framework object]</value>\n" +
            "    </data>\n" +
            "    <data name=\"Icon1\" type=\"System.Drawing.Icon, System.Drawing\" mimetype=\"application/x-microsoft.net.object.bytearray.base64\">\n" +
            "        <value>[base64 mime encoded string representing a byte array form of the .NET Framework object]</value>\n" +
            "        <comment>This is a comment</comment>\n" +
            "    </data>\n" +
            "                \n" +
            "    There are any number of \"resheader\" rows that contain simple \n" +
            "    name/value pairs.\n" +
            "    \n" +
            "    Each data row contains a name, and value. The row also contains a \n" +
            "    type or mimetype. Type corresponds to a .NET class that support \n" +
            "    text/value conversion through the TypeConverter architecture. \n" +
            "    Classes that don't support this are serialized and stored with the \n" +
            "    mimetype set.\n" +
            "    \n" +
            "    The mimetype is used for serialized objects, and tells the \n" +
            "    ResXResourceReader how to depersist the object. This is currently not \n" +
            "    extensible. For a given mimetype the value must be set accordingly:\n" +
            "    \n" +
            "    Note - application/x-microsoft.net.object.binary.base64 is the format \n" +
            "    that the ResXResourceWriter will generate, however the reader can \n" +
            "    read any of the formats listed below.\n" +
            "    \n" +
            "    mimetype: application/x-microsoft.net.object.binary.base64\n" +
            "    value   : The object must be serialized with \n" +
            "            : System.Runtime.Serialization.Formatters.Binary.BinaryFormatter\n" +
            "            : and then encoded with base64 encoding.\n" +
            "    \n" +
            "    mimetype: application/x-microsoft.net.object.soap.base64\n" +
            "    value   : The object must be serialized with \n" +
            "            : System.Runtime.Serialization.Formatters.Soap.SoapFormatter\n" +
            "            : and then encoded with base64 encoding.\n" +
            "\n" +
            "    mimetype: application/x-microsoft.net.object.bytearray.base64\n" +
            "    value   : The object must be serialized into a byte array \n" +
            "            : using a System.ComponentModel.TypeConverter\n" +
            "            : and then encoded with base64 encoding.\n" +
            "    -->\n" +
            "  <xsd:schema id=\"root\" xmlns=\"\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:msdata=\"urn:schemas-microsoft-com:xml-msdata\">\n" +
            "    <xsd:import namespace=\"http://www.w3.org/XML/1998/namespace\" />\n" +
            "    <xsd:element name=\"root\" msdata:IsDataSet=\"true\">\n" +
            "      <xsd:complexType>\n" +
            "        <xsd:choice maxOccurs=\"unbounded\">\n" +
            "          <xsd:element name=\"metadata\">\n" +
            "            <xsd:complexType>\n" +
            "              <xsd:sequence>\n" +
            "                <xsd:element name=\"value\" type=\"xsd:string\" minOccurs=\"0\" />\n" +
            "              </xsd:sequence>\n" +
            "              <xsd:attribute name=\"name\" use=\"required\" type=\"xsd:string\" />\n" +
            "              <xsd:attribute name=\"type\" type=\"xsd:string\" />\n" +
            "              <xsd:attribute name=\"mimetype\" type=\"xsd:string\" />\n" +
            "              <xsd:attribute ref=\"xml:space\" />\n" +
            "            </xsd:complexType>\n" +
            "          </xsd:element>\n" +
            "          <xsd:element name=\"assembly\">\n" +
            "            <xsd:complexType>\n" +
            "              <xsd:attribute name=\"alias\" type=\"xsd:string\" />\n" +
            "              <xsd:attribute name=\"name\" type=\"xsd:string\" />\n" +
            "            </xsd:complexType>\n" +
            "          </xsd:element>\n" +
            "          <xsd:element name=\"data\">\n" +
            "            <xsd:complexType>\n" +
            "              <xsd:sequence>\n" +
            "                <xsd:element name=\"value\" type=\"xsd:string\" minOccurs=\"0\" msdata:Ordinal=\"1\" />\n" +
            "                <xsd:element name=\"comment\" type=\"xsd:string\" minOccurs=\"0\" msdata:Ordinal=\"2\" />\n" +
            "              </xsd:sequence>\n" +
            "              <xsd:attribute name=\"name\" type=\"xsd:string\" use=\"required\" msdata:Ordinal=\"1\" />\n" +
            "              <xsd:attribute name=\"type\" type=\"xsd:string\" msdata:Ordinal=\"3\" />\n" +
            "              <xsd:attribute name=\"mimetype\" type=\"xsd:string\" msdata:Ordinal=\"4\" />\n" +
            "              <xsd:attribute ref=\"xml:space\" />\n" +
            "            </xsd:complexType>\n" +
            "          </xsd:element>\n" +
            "          <xsd:element name=\"resheader\">\n" +
            "            <xsd:complexType>\n" +
            "              <xsd:sequence>\n" +
            "                <xsd:element name=\"value\" type=\"xsd:string\" minOccurs=\"0\" msdata:Ordinal=\"1\" />\n" +
            "              </xsd:sequence>\n" +
            "              <xsd:attribute name=\"name\" type=\"xsd:string\" use=\"required\" />\n" +
            "            </xsd:complexType>\n" +
            "          </xsd:element>\n" +
            "        </xsd:choice>\n" +
            "      </xsd:complexType>\n" +
            "    </xsd:element>\n" +
            "  </xsd:schema>\n" +
            "  <resheader name=\"resmimetype\">\n" +
            "    <value>text/microsoft-resx</value>\n" +
            "  </resheader>\n" +
            "  <resheader name=\"version\">\n" +
            "    <value>2.0</value>\n" +
            "  </resheader>\n" +
            "  <resheader name=\"reader\">\n" +
            "    <value>System.Resources.ResXResourceReader, System.Windows.Forms, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089</value>\n" +
            "  </resheader>\n" +
            "  <resheader name=\"writer\">\n" +
            "    <value>System.Resources.ResXResourceWriter, System.Windows.Forms, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089</value>\n" +
            "  </resheader>\n" +
            "  <data name=\"ResourceFlowDirection\" xml:space=\"preserve\">\n" +
            "    <value>LeftToRight</value>\n" +
            "    <comment>Controls the FlowDirection for all elements in the RootFrame. Set to the traditional direction of this resource file's language</comment>\n" +
            "  </data>";

    public static final String WINDOWS_ENGLISH_OPENER =
            "<data name=\"ResourceLanguage\" xml:space=\"preserve\">\n" +
            "    <value>en-US</value>\n" +
            "    <comment>Controls the Language and ensures that the font for all elements in the RootFrame aligns with the app's language. Set to the language code of this resource file's language.</comment>\n" +
            "  </data>";

    public static final String WINDOWS_FRENCH_OPENER =
            "<data name=\"ResourceLanguage\" xml:space=\"preserve\">\n" +
                    "    <value>fr-CA</value>\n" +
                    "    <comment>Controls the Language and ensures that the font for all elements in the RootFrame aligns with the app's language. Set to the language code of this resource file's language.</comment>\n" +
                    "  </data>";

    public static final String WINDOWS_CLOSER = "</root>";

    public static void processWindowsStrings(List<Strings> strings)throws FileNotFoundException, UnsupportedEncodingException{
        //Add the XML header for Android files
        englishWriter.println(XML_OPENER);
        frenchWriter.println(XML_OPENER);

        //Add the Windows opening
        englishWriter.println(WINDOWS_FILE_OPENER);
        frenchWriter.println(WINDOWS_FILE_OPENER);

        //Add the language
        englishWriter.print(WINDOWS_ENGLISH_OPENER);
        frenchWriter.print(WINDOWS_FRENCH_OPENER);

        //Go through the strings
        for(Strings currentStrings : strings){
            try{
                lineNumber = strings.indexOf(currentStrings) + 2;

                //If there is no ID, we cannot parse it, so show a warning
                if(currentStrings.getKey() == null){
                    System.out.println("Warning! Line " + lineNumber + " has no ID, and therefore cannot be parsed");
                    continue;
                }

                String englishString = addWindowsEnglishString(currentStrings);
                String frenchString = addWindowsFrenchString(currentStrings);

                //If one is null, there is no value, so do not add it
                if(englishString != null){
                    englishWriter.println(englishString);
                }
                if(frenchString != null){
                    frenchWriter.println(frenchString);
                }
            }
            catch (Exception e){
                System.out.println("Error on Line " + lineNumber);
                e.printStackTrace();
            }
        }

        //Add the resources closing to android files
        englishWriter.println(WINDOWS_CLOSER);
        frenchWriter.println(WINDOWS_CLOSER);

        //Close the writers
        englishWriter.close();
        frenchWriter.close();
    }

    public static String addWindowsEnglishString(Strings strings){
        return addWindowsString(strings.getKey(), strings.getEn());
    }

    public static String addWindowsFrenchString(Strings strings){
        //For headers in the french XML
        if(strings.getKey().equalsIgnoreCase("header")){
            return addWindowsString(strings.getKey(), strings.getEn());
        }
        return addWindowsString(strings.getKey(), strings.getFr());
    }

    public static String addWindowsString(String key, String string){
        //First check if string is empty: if it is, return null
        if(string.isEmpty()){
            return null;
        }

        //Add initial indentation
        String xmlString = "  ";

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

            //Beginning of object (ID)
            xmlString += "<data name=\"" + key.trim() + "\" xml:space=\"preserve\">";
            //The value
            xmlString += "\n        <value>" + string + "</value>";
            //Closing Object
            xmlString += "\n  </data>";
        }
        return xmlString;
    }
}
