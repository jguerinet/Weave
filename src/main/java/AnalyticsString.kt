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
 * One Event/Screen Name to use for Analytics
 * @author Julien Guerinet
 * @since 5.0.0
 */
class AnalyticsString(
    key: String,
    url: String,
    lineNumber: Int,
    val type: AnalyticsType,
    val tag: String
) : BaseString(key, url, lineNumber)

enum class AnalyticsType {
    EVENT,
    SCREEN;

    companion object {

        /**
         * Parses the [string] into an [AnalyticsType]. Returns null if none found
         */
        fun parse(string: String?) = when {
            string.equals("Event", ignoreCase = true) -> EVENT
            string.equals("Screen", ignoreCase = true) -> SCREEN
            else -> null
        }
    }
}