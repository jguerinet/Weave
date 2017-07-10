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
 * One language that the Strings are in
 * @author Julien Guerinet
 * @since 2.0.0
 */
class Language {

    /**
     * Language Id (2 character representation, i.e. 'en')
     */
    private final String id;

    /**
     * Path to the file the Strings in this language should be stored in
     */
    private final String path;

    /**
     * Index of the column of this language in the CSV file
     */
    private int columnIndex;

    /**
     * Default Constructor
     *
     * @param id   Language Id
     * @param path Path to the file for the Strings in this language
     */
    Language(String id, String path) {
        this.id = id;
        this.path = path;
        // Column starts out as -1 until we find the right column in the Csv file
        columnIndex = -1;
    }

    /* GETTERS */

    /**
     * @return Language Id
     */
    String getId() {
        return id;
    }

    /**
     * @return Language path
     */
    String getPath() {
        return path;
    }

    /**
     * @return Column index of this language
     */
    int getColumnIndex() {
        return columnIndex;
    }

    /* SETTERS */

    /**
     * @param columnIndex Column index of this language
     */
    void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }
}
