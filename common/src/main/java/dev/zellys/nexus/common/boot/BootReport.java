/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.boot;

import java.util.List;

public final class BootReport {
    public record Integration(String name, boolean found) {
    }

    public record Module(String name, boolean enabled, long millis) {
    }

    private final String version;
    private final List<Integration> integrations;
    private final List<Module> modules;

    public BootReport(String version, List<Integration> integrations, List<Module> modules) {
        this.version = version;
        this.integrations = List.copyOf(integrations);
        this.modules = List.copyOf(modules);
    }

    public String version() {
        return version;
    }

    public List<Integration> integrations() {
        return integrations;
    }

    public List<Module> modules() {
        return modules;
    }
}
