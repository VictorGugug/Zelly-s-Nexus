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

import dev.zellys.nexus.common.boot.BootPrinter;
import dev.zellys.nexus.common.boot.BootReport;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZellysNexus extends JavaPlugin {
    private Translator translator;

    @Override
    public void onEnable() {
        long start = System.nanoTime();
        translator = new Translator("en");
        long translationMs = (System.nanoTime() - start) / 1_000_000;
        List<BootReport.Integration> integrations = List.of(
                integration("LuckPerms"),
                integration("Geyser-Spigot"),
                integration("Floodgate"),
                integration("PlaceholderAPI"));
        List<BootReport.Module> modules = new ArrayList<>();
        modules.add(new BootReport.Module("translation", true, translationMs));
        modules.add(new BootReport.Module("updater", false, 0));
        modules.add(new BootReport.Module("auth", false, 0));
        modules.add(new BootReport.Module("antibot", false, 0));
        modules.add(new BootReport.Module("tab", false, 0));
        modules.add(new BootReport.Module("essentials", false, 0));
        modules.add(new BootReport.Module("chat", false, 0));
        modules.add(new BootReport.Module("inventory", false, 0));
        modules.add(new BootReport.Module("discord", false, 0));
        modules.add(new BootReport.Module("clans", false, 0));
        modules.add(new BootReport.Module("itemedit", false, 0));
        BootReport report = new BootReport(getDescription().getVersion(), integrations, modules);
        getLogger().info(translator.get(TranslationKey.BOOT_VERSION, report.version()));
        for (String line : BootPrinter.render(report, translator)) {
            getLogger().info(line);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info(translator.get(TranslationKey.PLUGIN_DISABLED));
    }

    public Translator translator() {
        return translator;
    }

    private BootReport.Integration integration(String name) {
        Plugin plugin = getServer().getPluginManager().getPlugin(name);
        return new BootReport.Integration(name, plugin != null && plugin.isEnabled());
    }
}
