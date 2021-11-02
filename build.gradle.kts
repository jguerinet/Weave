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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

plugins {
    id("application")
    id(Deps.Plugins.VERSIONS) version Versions.Plugins.VERSIONS
    id(Deps.Plugins.SPOTLESS) version Versions.Plugins.SPOTLESS
    kotlin("jvm") version Versions.KOTLIN
    kotlin("plugin.serialization") version Versions.KOTLIN
}

buildscript {

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", Versions.KOTLIN))
    }
}

version = Versions.WEAVE

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation(Deps.KOTLINX_SERIALIZATION)
    implementation(Deps.Square.OKHTTP)
    implementation(Deps.Square.OKIO)
    implementation(Deps.SUPER_CSV)
}

val fatJar = task("fatJar", type = Jar::class) {
    manifest {
        attributes["Main-Class"] = "com.guerinet.weave.Weave"
    }
    from(sourceSets.main.get().output)
    // from(configurations.runtime.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec)
}

/* Versions Configuration */

tasks.named(
    "dependencyUpdates",
    com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class.java
).configure {
    // Don't allow unstable versions if the current version is stable
    rejectVersionIf {
        isUnstable(candidate.version) && !isUnstable(currentVersion)
    }
}

/**
 * Returns true if the [version] is unstable, false otherwise
 */
fun isUnstable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return !stableKeyword && !regex.matches(version)
}

/* Spotless Configuration */

spotless {

    format("misc") {
        target("**/.gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("markdown") {
        target("**/*.md")
        trimTrailingWhitespace()
        endWithNewline()
        prettier().config(mapOf("parser" to "markdown", "tabWidth" to 4))
    }

    kotlin {
        target("**/*.kt")
        ktlint(Versions.Plugins.KTLINT)
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint(Versions.Plugins.KTLINT)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// apply from: "https://raw.githubusercontent.com/jguerinet/Gradle-Artifact-Scripts/master/kotlin-artifacts.gradle"
