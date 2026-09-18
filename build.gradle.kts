// Copyright (c) 2026 Zar
//
// This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
//
// Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
// plus this project's additional terms. Both are included in full in the
// LICENSE file at the root of this repository.

import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    `java-library`
}

val nexusVersion: String = project.property("nexusVersion") as String
val versionSuffix: String = (project.property("versionSuffix") as String).trim()
val fullVersion: String = if (versionSuffix.isBlank()) nexusVersion else "$nexusVersion-$versionSuffix"

allprojects {
    apply(plugin = "org.gradle.java-library")

    group = "dev.zellys.nexus"
    version = fullVersion

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
