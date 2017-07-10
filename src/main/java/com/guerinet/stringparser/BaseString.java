/*
 * Copyright 2013-2017 Julien Guerinet
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
 * Basic String information. Will be used for headers (Key will be a comment) and subclasses by
 *  the actual Strings
 * @author Julien Guerinet
 * @since 2.6.0
 */
class BaseString {

    /**
     * Key to store the String under, or the header to use
     */
    private final String key;

    /**
     * Url of the file this String comes from
     */
    private final String url;

    /**
     * Line number in the CSV that this String was on
     */
    private final int lineNumber;

    /**
     * Default Constructor
     *
     * @param key        Key to store the String under, or header String
     * @param url        Url of the file this String comes from
     * @param lineNumber Line number in the CSV that this String was on
     */
    BaseString(String key, String url, int lineNumber) {
        this.key = key;
        this.url = url;
        this.lineNumber = lineNumber;
    }

    /* GETTERS */

    /**
     * @return String Key
     */
    String getKey() {
        return key;
    }

    /**
     * @return Url of the file this String comes from
     */
    String getUrl() {
        return url;
    }

    /**
     * @return Line number in the CSV that this String was on
     */
    int getLineNumber() {
        return lineNumber;
    }
}
