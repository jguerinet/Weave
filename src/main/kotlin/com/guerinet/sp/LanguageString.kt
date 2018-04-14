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

package com.guerinet.sp

/**
 * One String with all of the translations
 * @author Julien Guerinet
 * @since 1.0.0
 */
class LanguageString(key: String, url: String, lineNumber: Int) : BaseString(key, url, lineNumber) {

    /**
     * Maps of translations, the keys being the language Id and the values being the String
     */
    private val translations = mutableMapOf<String, String>()

    /**
     * List of platforms this String is for. If this is empty, it's for all platforms
     */
    private val platforms = mutableListOf<String>()

    /**
     * Returns the String for the given language [id], null if none
     */
    fun getString(id: String) = translations[id]

    /**
     * Adds a [string] translation for the given language [id]
     */
    fun addTranslation(id: String, string: String) = translations.put(id, string)

    /**
     * Returns true if this string is for the given [platform], false otherwise
     */
    fun isForPlatform(platform: String) = platforms.isEmpty() || platforms.contains(platform)

    /**
     * Adds the given platforms in the [platformCsv] to the list of platforms
     */
    fun addPlatforms(platformCsv: String?) =
            platformCsv?.split(",")?.forEach { platforms.add(it.trim().toLowerCase()) }
}