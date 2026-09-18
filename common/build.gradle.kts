// Copyright (c) 2026 Zar
//
// This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
//
// Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
// plus this project's additional terms. Both are included in full in the
// LICENSE file at the root of this repository.

dependencies {
    implementation("org.yaml:snakeyaml:2.7")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
