/*
 * Copyright 2013-2020 Julien Guerinet
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

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

/**
 * One language that the Strings are in
 * @author Julien Guerinet
 * @since 2.0.0
 *
 * @param id Language Id
 * @param path Path to the file for the Strings in this language
 * @param columnIndex Index of the column of this language in the CSV file (starts as -1)
 */
@Serializable
class Language(@Required val id: String, @Required val path: String, var columnIndex: Int = -1) {

    companion object {

        /** Dummy [Language]  object to use when we do not care about the language */
        val EMPTY = Language("en", "", 0)
    }
}
