/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus;

import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZellysNexus extends JavaPlugin {
    private Translator translator;

    @Override
    public void onEnable() {
        translator = new Translator("en");
        getLogger().info(translator.get(TranslationKey.BOOT_TITLE));
        getLogger().info(translator.get(TranslationKey.BOOT_VERSION, getDescription().getVersion()));
        getLogger().info(translator.get(TranslationKey.BOOT_PLATFORM));
        getLogger().info(translator.get(TranslationKey.BOOT_LANGUAGE));
    }

    @Override
    public void onDisable() {
        getLogger().info(translator.get(TranslationKey.PLUGIN_DISABLED));
    }

    public Translator translator() {
        return translator;
    }
}
