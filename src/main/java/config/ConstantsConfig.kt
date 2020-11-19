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

package com.guerinet.weave.config

import com.guerinet.weave.config.ConstantsConfig.Casing
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
 * @param typeCasing [Casing] to name the types, defaults to Pascal
 * @param keyCasing [Casing] to name the keys, defaults to camel
 * @param isTopLevelClassCreated True if there should be a top level class created from the file name, false otherwise
 *                                  Defaults to true
 */
@Serializable
class ConstantsConfig(
    @Required val title: String,
    @Required val sources: List<Source> = listOf(),
    @Required val path: String = "",
    val packageName: String? = null,
    val typeColumnName: String = "",
    val valueColumnName: String = "value",
    val valuesAlignColumn: Int = 0,
    val typeCasing: Casing = Casing.PASCAL_CASE,
    val keyCasing: Casing = Casing.CAMEL_CASE,
    val isTopLevelClassCreated: Boolean = true
) {
    /**
     * Casing to use for naming the tags
     */
    @Serializable(with = Casing.Companion::class)
    enum class Casing {
        NONE,
        CAMEL_CASE,
        PASCAL_CASE,
        SNAKE_CASE,
        CAPS;

        companion object : KSerializer<Casing> {

            override val descriptor: SerialDescriptor =
                PrimitiveSerialDescriptor("Casing", PrimitiveKind.STRING)

            override fun deserialize(decoder: Decoder): Casing = when (decoder.decodeString().toLowerCase()) {
                "camel", "camelcase" -> CAMEL_CASE
                "pascal", "pascalcase" -> PASCAL_CASE
                "snake", "snakecase" -> SNAKE_CASE
                "caps" -> CAPS
                else -> NONE
            }

            override fun serialize(encoder: Encoder, value: Casing) = error("This object should not be serialized")
        }
    }
}
