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

/**
 * A String used for a header within the Strings file. Will be a comment within the Strings file
 * @author Julien Guerinet
 * @since 2.6.0
 */
public class HeaderString {
    /**
     * The key to store the String under, or the header to use
     */
    protected String key;

    /**
     * The line number in the CSV that this String was on
     */
    protected int lineNumber;

    /**
     * Default Constructor
     *
     * @param header     The header comment
     * @param lineNumber Line number in the CSV that this String was on
     */
    public HeaderString(String header, int lineNumber){
        this.key = header;
        this.lineNumber = lineNumber;
    }

    /* GETTERS */

    /**
     * @return The String Key
     */
    public String getKey(){
        return this.key;
    }

    /**
     * @return The line number in the CSV that this String was on
     */
    public int getLineNumber(){
        return lineNumber;
    }
}
