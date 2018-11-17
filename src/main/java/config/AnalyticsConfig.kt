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

package config

import com.guerinet.weave.config.Source
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

/**
 * Base parsed Config Json
 * @author Julien Guerinet
 * @since 5.0.0
 */
@Serializable
class AnalyticsConfig {

    /** List of [Source]s the Strings are coming from */
    val sources: List<Source> = listOf()

    /** Path to the file to write to */
    val path: String = ""

    /** Optional package name used on Android */
    @Optional
    val packageName: String? = null

    /** Name of the column that holds the type */
    val typeColumnName: String = ""

    /** Name of the column that holds the tag */
    val tagColumnName: String = ""
}