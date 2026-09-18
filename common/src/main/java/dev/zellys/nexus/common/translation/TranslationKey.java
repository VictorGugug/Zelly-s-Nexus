/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.translation;

public enum TranslationKey {
    PREFIX("prefix"),
    BOOT_VERSION("boot.version"),
    BOOT_PLATFORM("boot.platform"),
    BOOT_LANGUAGE("boot.language"),
    BOOT_TAGLINE_1("boot.tagline1"),
    BOOT_TAGLINE_2("boot.tagline2"),
    BOOT_DETECTING("boot.detecting"),
    BOOT_MANAGER("boot.manager"),
    STATUS_LOADED_IN("status.loaded_in"),
    STATUS_DISABLED("status.disabled"),
    STATUS_FOUND("status.found"),
    STATUS_NOT_FOUND("status.not_found"),
    PLUGIN_DISABLED("plugin.disabled");

    private final String key;

    TranslationKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
