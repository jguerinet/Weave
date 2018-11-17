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

package com.guerinet.weave.config

import kotlinx.serialization.Serializable

/**
 * One source from where the Strings could be coming from
 * @author Julien Guerinet
 * @since 5.0.0
 *
 * @param title Name of this source (for logging purposes)
 * @param url   Url to retrieve the Strings from
 */
@Serializable
class Source(val title: String, val url: String)