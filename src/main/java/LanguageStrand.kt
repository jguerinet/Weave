/*
 * Copyright 2013-2018 Julien Guerinet
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

package com.guerinet.weave

/**
 * One String with all of the translations
 * @author Julien Guerinet
 * @since 1.0.0
 */
class LanguageStrand(
    key: String,
    sourceName: String,
    lineNumber: Int
) : BaseStrand(key, sourceName, lineNumber) {

    /**
     * Maps of translations, the keys being the language Id and the values being the String
     */
    val translations = mutableMapOf<String, String>()

    /**
     * Returns the String for the given language [id], null if none
     */
    fun getString(id: String) = translations[id]

    /**
     * Adds a [string] translation for the given language [id]
     */
    fun addTranslation(id: String, string: String) = translations.put(id, string)
}