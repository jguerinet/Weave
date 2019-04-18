# Weave

## Summary

Weave takes an online CSV file and parses the content to produce the Strings files for an Android or iOS app. It also parses the Strings for Web in the form of a JSON: an on object with a bunch of key-value pairs, the key being the String key and the value being the translated String for that language.

It can also parse Constants into files that you are using the same properties cross platform (ex: Analytics tags, Regex expressions).

It will also show any warnings or errors that the Strings file might have.
Please note that it assumes that the file is in UTF-8 and also produces UTF-8 files.

## Instructions

To use this:

-   Download the latest jar from the releases tab and the [sample config](weave-config-sample.json)
-   Set up your config file (fill out the fields in the config). The following fields are optional:
    -   `headerColumnName`, which represents the symbol to show that a line is a header, defaults to `###`.
    -   `keyColumnName`, which represents the name of the column where the key is stored, defaults to `key`
    -   `platformsColumnName`, which represents the name of the column where the supported platforms for a specific row is, defaults to `platforms`
-   Any columns containing translations in your CSV file must have a header with the 2 character language Id (ex: en, fr)
-   You may also add a 'platforms' column and put in a CSV list of the platforms that a particular String should be parsed for. If the column doesn't exist or the platform value is empty for a specific String it will be parsed for all platforms
-   Keys must be unique, not have spaces, and not be null (the parser will inform of any of these errors when you run it)
-   You can add headers (which will be parsed as comments) in your Strings file by adding or surrounding your header with '###' (or whatever you specify).For example, if you put ### General ### (or ### General), it will be parsed as `/* General */` on iOS and `<!-- General -->` on Android
-   You can add formatted Strings by putting either the normal placeholder (i.e. %s for a String) if there is one argument, or numbering them
    like this for multiple: %1$s, %2$s...
-   Run the jar

For the constants:

-   Fill out the same sample config. You can add as many configs as you want to the `constants` array.
-   This will generate an `object` on Android / `class` on iOS / Json object on Web.
    Within this object, you can have enums for the different subclasses using the `type` functionality (if not they will be top level constants). The key will be used as variable names (optionally capitalized on mobile), and the tag will be the value.
-   You can change the casing of both the types and values to be none (leave as is), camel (capitalize each word except first), pascal (capitalize each word), snake (lowercase everything, underscores for spaces), and caps (capitalize everything, underscores for spaces). Note, for this to work you need to separate each word by a space in your CSV.

## Gradle Dependencies

-   [OkHttp](http://square.github.io/okhttp/)
-   [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization)
-   [super-csv](http://super-csv.github.io/super-csv/)

## Contributors

-   [Julien Guerinet](https://github.com/jguerinet)
-   [Yulric Sequeira](https://github.com/yulric)

## Version History

See the [Change Log](CHANGELOG.md).

## Copyright

    Copyright 2013-2019 Julien Guerinet

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
