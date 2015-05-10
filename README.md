# Mobile String Parser

## Summary
The Mobile String Parser takes an online CSV file and parses the content to produce the Strings files for an Android or iOS app.
It will also show any warnings or errors that the Strings file might have. 
Please note that it assumes that the file is in UTF-8 and also produces UTF-8 files. 

## Instructions
To use this: 

* Download the [jar][1] and the [sample config][2].
* Set up your config file (follow the instructions in sample-config.txt)
* The first column of your CSV file must be the keys with the header 'key' 
* Any columns containing translations in your CSV file must have a header with the 2 character language Id (ex: en, fr)
* Keys must be unique, not have spaces, and not be null (the parser will inform of any of these errors when you run it) 
* Run the jar

[1]:https://raw.githubusercontent.com/jguerinet/mobile-string-parser/dev/mobile-string-parser-2.5.jar
[2]:https://raw.githubusercontent.com/jguerinet/mobile-string-parser/dev/sample-config.txt

## Branches
* master: Contains the main code 
* dev: Contains WIP code

## Gradle Dependencies
* okhttp:       HTTP client
* super-csv:    CSV parsing

## Contributors
* Julien Guerinet
* Yulric Sequeira 

## Version History
* Version 2.5 - May 10, 2015 (Moved to Gradle based project)
* Version 2.4 - May 05, 2015 (Fixed encoding bug and started using okio/okhttp)
* Version 2.3 - April 18, 2015 (Fixed key trimming bug)
* Version 2.2 - April 16, 2015 (Added logging and connection checking)
* Version 2.1 - April 15, 2015 (Removing new line characters)
* Version 2.0 - April 08, 2015 (Multiple Language Support, Code Cleanup)
* Version 1.3 - February 20, 2015 (Escaping Quotations)
* Version 1.2 - June 02, 2014 (Error and Warning Messages)
* Version 1.1 - May 27, 2014 (Windows Phone Code Addition)
* Version 1.0 - May 02, 2014 (Initial Code)

##Copyright 
    Copyright 2013-2015 Julien Guerinet

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.