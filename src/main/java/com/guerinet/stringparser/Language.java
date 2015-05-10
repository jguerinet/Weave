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
 * One language that the Strings are in
 * @author Julien Guerinet
 * @version 2.4
 * @since 2.0
 */
public class Language {
    /**
     * The language Id
     */
    private String id;
    /**
     * The language path
     */
    private String path;
    /**
     * The column index of the language in the CSV file
     */
    private int columnIndex;

    /**
     * Default Constructor
     *
     * @param id   The language Id
     * @param path The language path
     */
    public Language(String id, String path){
        this.id = id;
        this.path = path;
        this.columnIndex = -1;
    }

    /* GETTERS */

    /**
     * @return The language Id
     */
    public String getId(){
        return this.id;
    }

    /**
     * @return The language path
     */
    public String getPath(){
        return this.path;
    }

    /**
     * @return The column index of this language
     */
    public int getColumnIndex(){
        return this.columnIndex;
    }

    /* SETTERS */

    /**
     * @param columnIndex The column index of this language
     */
    public void setColumnIndex(int columnIndex){
        this.columnIndex = columnIndex;
    }
}
