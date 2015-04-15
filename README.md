# String Parser

## Version History
* Version 2.1 - April 15, 2015 (Removing new line characters)
* Version 2.0 - April 08, 2015 (Multiple Language Support, Code Cleanup)
* Version 1.3 - February 20, 2015 (Escaping Quotations)
* Version 1.2 - June 02, 2014 (Error and Warning Messages)
* Version 1.1 - May 27, 2014 (Windows Phone Code Addition)
* Version 1.0 - May 02, 2014 (Initial Code)

## Summary
The String Parser takes an online CSV file and parses the content to produce the Strings files for and Android or iOS app.
It will also show any warnings or errors that the Strings file might have. 

## Instructions
To use this: 

* Set up your config file (follow the instructions in Sample Config.txt)
* Have a URL to an online CSV file (recommended: Google Drive file) 
* The first column of your file must be the keys
* Any columns containing translations must have a header with the 2 character language Id (ex: en, fr)
* Keys must be unique, not have spaces, and not be null

## Branches
* master: Contains the main code 
* dev: Contains WIP code

## Directories
* libs: All of the necessary external libraries (in jar format)
* src: The source code

## Contributors
* Julien Guerinet

##Copyright 
Copyright (c) Julien Guerinet. All rights reserved.