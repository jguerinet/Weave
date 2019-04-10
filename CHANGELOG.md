# Change Log

## Version 7.0.0 (2019-04-10)

-   Renamed the entire Analytics section to be Constants
-   Added the capacity to have multiple ConstantsConfigs for different types of Constants
-   Removed the `types` array in the `ConstantConfig`, the types will be grabbed from the Csv
-   Fixed bug where some optional fields were required
-   Added documentation

## Version 6.0.0 (2019-04-08)

-   For Analytics, added option to specify which column the mobile analytics should align to
-   For Analytics, added option to not capitalize the variable names on mobile
-   For Analytics, added option to not have a top level class on mobile
-   For Strings, added a top level language object for Web
-   For Strings, fixed spacing bug in Android
-   For Strings, fixed percentages on iOS and Android
-   For Strings, fixed formatted Strings on Web

## Version 5.2.0 (2018-12-03)

-   Changed the duplicate verification for `AnalyticsStrand`s such that it needs to be the same key **and** the same type for it to be considered a duplicate

## Version 5.1.3 (2018-11-21)

-   Fixed header parsing so that you can use the Id column for other values

## Version 5.1.2 (2018-11-21)

-   Switched the `AnalyticsConfig` to be a constructor based class

## Version 5.1.1 (2018-11-21)

-   Fixed `AnalyticsConfig` package

## Version 5.1.0 (2018-11-21)

-   Made the Analytics parsing part of Weave much more flexible. Categories can now be anything, and are optional
-   Added a way to override the key column name, the platforms column name, and the header symbol
-   Made Json parsing lenient, which means you can have extra fields within your config

## Version 5.0.0 (2018-11-17)

-   Library is now in Kotlin
-   Set up Gradle publishing so it can be used as a dependency for custom implementations
-   Switched to a Json based config
-   Added parsing of Analytic events and screens
-   Renamed the library
-   Fixed dashes in Android

## Version 4.0.4 (2017-07-14)

-   Made a bunch of classes and methods public

## Version 4.0.3 (2017-07-13)

-   Reworked the way Strings were written
-   Pulled out some more methods for easier overriding

## Version 4.0.2 (2017-07-13)

-   Switched to using an instance variable
-   Broke up the main method into smaller functional methods
-   Made all functions and variables protected
-   Set up project to be published to JitPack

## Version 4.0.1 (2017-07-13)

-   Fixed bug where null header cells would cause the app to crash

## Version 4.0.0 (2017-07-10)

-   Reworked the entire String formatting section to remove redundant code
-   Added support for multiple Csv files from multiple Urls
-   Added support for platform specific Strings
-   Added checks for illegal characters within the String keys
-   Added support for a different column order (the keys column doesn't need to be first anymore)

## Version 3.1.0 (2017-04-26)

-   Switched the web parsing to work like the other platforms (1 file per language)
-   Added warnings for when there is a translation missing

## Version 3.0.0 (2017-02-15)

-   Added String parsing for web

## Version 2.8.1 (2015-12-02)

-   Fixed bug where the HTML tags would be parsed after the "<" and ">" symbols in Android

## Version 2.8.0 (2015-10-06)

-   Added correct parsing for "<" and ">" in Android

## Version 2.7.0 (2015-09-21)

-   Fixed bug regarding single quotation marks in parsing iOS Strings
-   Changed header identifier to be less common

## Version 2.6.0 (2015-05-20)

-   Added the external dependencies to the generated jar

## Version 2.5.0 (2015-05-10)

-   Moved to Gradle based project
-   Moved to Java 7

## Version 2.4.0 (2015-05-05)

-   Starting using OkHttp as the HTTP client
-   Fixed encoding bug

## Version 2.3.0 (2015-04-18)

-   Fixed key trimming bug

## Version 2.2.0 (2015-04-16)

-   Added logging
-   Added checking the connection before continuing

## Version 2.1.0 (2015-04-15)

-   Added code to remove the new line character from strings

## Version 2.0.0 (2015-04-08)

-   Completely rewrote the parser for it to be more efficient and contain less boilerplate code
-   Added the support of a dynamic number and types of languages (as opposed to forcing en and fr)

## Version 1.3.0 (2015-02-20)

-   Added escaping for quotations

## Version 1.2.0 (2014-06-02)

-   Added exception and warning logging

## Version 1.1.0 (2014-05-27)

-   Added parsing for Windows Phone

## Version 1.0.0 (2014-05-02)

-   Initial Release
