# Change Log

## Version 4.0.1 (2017-07-13)
* Fixed bug where null header cells would cause the app to crash

## Version 4.0.0 (2017-07-10)
* Reworked the entire String formatting section to remove redundant code
* Added support for multiple Csv files from multiple Urls
* Added support for platform specific Strings
* Added checks for illegal characters within the String keys
* Added support for a different column order (the keys column doesn't need to be first anymore) 

## Version 3.1.0 (2017-04-26)
* Switched the web parsing to work like the other platforms (1 file per language) 
* Added warnings for when there is a translation missing

## Version 3.0.0 (2017-02-15)
* Added String parsing for web

## Version 2.8.1 (2015-12-02)
* Fixed bug where the HTML tags would be parsed after the "<" and ">" symbols in Android

## Version 2.8.0 (2015-10-06)
* Added correct parsing for "<" and ">" in Android

## Version 2.7.0 (2015-09-21)
* Fixed bug regarding single quotation marks in parsing iOS Strings
* Changed header identifier to be less common 

## Version 2.6.0 (2015-05-20)
* Added the external dependencies to the generated jar

## Version 2.5.0 (2015-05-10)
* Moved to Gradle based project
* Moved to Java 7

## Version 2.4.0 (2015-05-05)
* Starting using OkHttp as the HTTP client
* Fixed encoding bug

## Version 2.3.0 (2015-04-18)
* Fixed key trimming bug

## Version 2.2.0 (2015-04-16)
* Added logging
* Added checking the connection before continuing

## Version 2.1.0 (2015-04-15)
* Added code to remove the new line character from strings

## Version 2.0.0 (2015-04-08)
* Completely rewrote the parser for it to be more efficient and contain less boilerplate code
* Added the support of a dynamic number and types of languages (as opposed to forcing en and fr)

## Version 1.3.0 (2015-02-20)
* Added escaping for quotations

## Version 1.2.0 (2014-06-02)
* Added exception and warning logging

## Version 1.1.0 (2014-05-27)
* Added parsing for Windows Phone

## Version 1.0.0 (2014-05-02)
* Initial Release