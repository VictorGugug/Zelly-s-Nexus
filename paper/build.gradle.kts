// Copyright (c) 2026 Zar
//
// This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
//
// Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
// plus this project's additional terms. Both are included in full in the
// LICENSE file at the root of this repository.

import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":common"))
    implementation("at.favre.lib:bcrypt:0.10.2")
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.papermc.paper:paper-api:26.3.build.+")
}

tasks {
    named<AbstractArchiveTask>("jar") {
        isEnabled = false
    }
    named<AbstractArchiveTask>("shadowJar") {
        archiveBaseName.set("ZellysNexus")
        archiveClassifier.set("")
    }
    named<ProcessResources>("processResources") {
        val replacements = mapOf("version" to project.version.toString())
        inputs.properties(replacements)
        filesMatching("paper-plugin.yml") {
            expand(replacements)
        }
    }
    runServer {
        minecraftVersion("26.3")
    }
}
