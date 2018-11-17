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

/**
 * Basic String information. Super class for al types of Strings
 * @author Julien Guerinet
 * @since 2.6.0
 *
 * @param key           Key to store the String under, or the header to use
 * @param url           Url of the file this String comes from
 * @param lineNumber    Line number in the CSV that this String was on
 * @param platformCsv   Nullable Csv String of the list of platforms this is allowed to be on. Defaults to null
 */
open class BaseString(
    val key: String,
    val url: String,
    val lineNumber: Int,
    platformCsv: String? = null
) {

    private val platforms: List<String> = platformCsv?.split(",")?.map { it.trim().toLowerCase() } ?: listOf()

    /**
     * Returns true if this string is for the given [platform], false otherwise
     */
    fun isForPlatform(platform: String) = platforms.isEmpty() || platforms.contains(platform.toLowerCase())
}