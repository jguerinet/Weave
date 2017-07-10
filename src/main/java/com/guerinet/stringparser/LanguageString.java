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

import java.util.HashMap;
import java.util.Map;

/**
 * One String with all of the translations
 * @author Julien Guerinet
 * @since 1.0.0
 */
class LanguageString extends BaseString {

    /**
     * Map of translations, with the key being the language Id and the value being the String
     */
    private final Map<String, String> translations;

    /**
     * Default Constructor
     *
     * @param key        String key
     * @param lineNumber Line number in the CSV that this String was on
     */
    LanguageString(String key, int lineNumber) {
        super(key, lineNumber);
        translations = new HashMap<>();
    }

    /* GETTERS */

    /**
     * @param id Language Id
     * @return String in that language
     */
    String getString(String id) {
        return translations.get(id);
    }

    /* SETTERS */

    /**
     * @param id     Language Id
     * @param string String
     */
    void addTranslation(String id, String string) {
        translations.put(id, string);
    }
}
