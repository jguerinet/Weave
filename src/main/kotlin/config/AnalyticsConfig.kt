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

import com.guerinet.sp.config.Source
import kotlinx.serialization.Serializable

/**
 * Base parsed Config Json
 * @author Julien Guerinet
 * @since 5.0.0
 */
@Serializable
open class AnalyticsConfig {

    /** Platform we are parsing for (Android, iOS, Web) */
    val platform: String = ""

    /** List of [Source]s the Strings are coming from */
    val sources: List<Source> = listOf()
}