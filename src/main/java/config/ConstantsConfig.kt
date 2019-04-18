/*
 * Copyright 2013-2019 Julien Guerinet
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

package com.guerinet.weave.config

import com.guerinet.weave.config.ConstantsConfig.Mode
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.withName

/**
 * Config for a list of constants (ex: Analytics)
 * @author Julien Guerinet
 * @since 5.0.0
 *
 * @param title Name of this set of Constants, used for debugging
 * @param sources List of [Source]s the Constants are coming from
 * @param path Path to the file to write to
 * @param packageName Optional package name used on Android
 * @param typeColumnName Name of the column that holds the type
 * @param valueColumnName Name of the column that holds the key
 * @param valuesAlignColumn Number of tabs to do in order to align the keys. Defaults to 0 (unaligned)
 * @param mode [Mode] to name the keys
 * @param isTopLevelClassCreated True if there should be a top level class created from the file name, false otherwise
 *                                  Defaults to true
 */
@Serializable
class ConstantsConfig(
    val title: String,
    val sources: List<Source> = listOf(),
    val path: String = "",
    @Optional val packageName: String? = null,
    @Optional val keyColumnName: String = "key",
    @Optional val typeColumnName: String = "",
    @Optional val valueColumnName: String = "value",
    @Optional val valuesAlignColumn: Int = 0,
    @Optional val mode: Mode = Mode.CAMEL_CASE,
    @Optional val isTopLevelClassCreated: Boolean = true
) {
    /**
     * Mode to use for naming the tags
     */
    enum class Mode {
        NONE,
        CAMEL_CASE,
        PASCAL_CASE,
        SNAKE_CASE;

        @Serializer(forClass = Mode::class)
        companion object : KSerializer<Mode> {

            override val descriptor: SerialDescriptor = StringDescriptor.withName("Mode")

            override fun deserialize(decoder: Decoder): Mode = when (decoder.decodeString().toLowerCase()) {
                "camel", "camelcase" -> CAMEL_CASE
                "pascal", "pascalcase" -> PASCAL_CASE
                "snake", "snakecase" -> SNAKE_CASE
                else -> NONE
            }

            override fun serialize(encoder: Encoder, obj: Mode) = error("This object should not be serialized")
        }
    }
}
