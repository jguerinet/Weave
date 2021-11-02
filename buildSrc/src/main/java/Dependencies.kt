/*
 * Copyright 2013-2021 Julien Guerinet
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

object Versions {

    const val WEAVE = "9.0.0"
    const val KOTLIN = "1.5.31"

    /* Plugins */
    object Plugins {
        const val KTLINT = "0.41.0"
        const val SPOTLESS = "5.14.2"
        const val VERSIONS = "0.39.0"
    }

    object Square {
        const val OKHTTP = "2.7.5"
        const val OKIO = "2.9.0"
    }

    const val KOTLINX_SERIALIZATION = "1.3.0"
    const val SUPER_CSV = "2.4.0"
}

object Deps {
    private const val SERIALIZATION_BASE = "org.jetbrains.kotlinx:kotlinx-serialization"

    object Plugins {
        const val KOTLINX_SERIALIZATION = "$SERIALIZATION_BASE:${Versions.KOTLIN}"
        const val SPOTLESS = "com.diffplug.spotless"
        const val VERSIONS = "com.github.ben-manes.versions"
    }

    object Square {
        private const val BASE = "com.squareup"
        const val OKHTTP = "$BASE.okhttp:okhttp:${Versions.Square.OKHTTP}"
        const val OKIO = "$BASE.okio:okio:${Versions.Square.OKIO}"
    }

    const val KOTLINX_SERIALIZATION = "$SERIALIZATION_BASE-json:${Versions.KOTLINX_SERIALIZATION}"
    const val SUPER_CSV = "net.sf.supercsv:super-csv:${Versions.SUPER_CSV}"
}
