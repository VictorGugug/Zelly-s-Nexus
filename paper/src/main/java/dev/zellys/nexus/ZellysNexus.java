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

import dev.zellys.nexus.antibot.AntibotModule;
import dev.zellys.nexus.auth.AuthModule;
import dev.zellys.nexus.boot.PaperBoot;
import dev.zellys.nexus.chat.ChatModule;
import dev.zellys.nexus.clans.ClanModule;
import dev.zellys.nexus.common.boot.BootPrinter;
import dev.zellys.nexus.common.boot.BootReport;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import dev.zellys.nexus.common.update.GitHubTagSource;
import dev.zellys.nexus.common.update.UpdateChecker;
import dev.zellys.nexus.dialog.DialogManager;
import dev.zellys.nexus.essentials.EssentialsModule;
import dev.zellys.nexus.inventory.InventoryModule;
import dev.zellys.nexus.itemedit.ItemEditModule;
import dev.zellys.nexus.integration.IntegrationsModule;
import dev.zellys.nexus.tab.TabModule;
import dev.zellys.nexus.tab.TabService;
import dev.zellys.nexus.command.ZnRootCommand;
import dev.zellys.nexus.debug.DebugModule;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;

public final class ZellysNexus extends JavaPlugin {
    private Translator translator;
    private DialogManager dialogs;
    private TabService tab;
    private ZnRootCommand znCommand;

    @Override
    public void onEnable() {
        long start = System.nanoTime();
        translator = new Translator("en");
        dialogs = new DialogManager(translator);
        znCommand = new ZnRootCommand(translator);
        
        this.registerCommand("zn", znCommand);
        
        long translationMs = (System.nanoTime() - start) / 1_000_000;
        List<BootReport.Integration> integrations = List.of(
                integration("LuckPerms"),
                integration("Geyser-Spigot"),
                integration("Floodgate"),
                integration("PlaceholderAPI"));
        List<BootReport.Module> modules = new ArrayList<>();
        modules.add(new BootReport.Module("translation", true, translationMs));
        modules.add(module("updater", () -> {
            checkForUpdates();
            return 0L;
        }));
        modules.add(module("auth", () -> AuthModule.enable(this, znCommand)));
        modules.add(module("antibot", () -> AntibotModule.enable(this, znCommand)));
        modules.add(module("tab", () -> {
            long begun = System.nanoTime();
            tab = TabModule.enable(this, znCommand);
            return (System.nanoTime() - begun) / 1_000_000;
        }));
        modules.add(module("essentials", () -> EssentialsModule.enable(this, znCommand)));
        modules.add(module("chat", () -> ChatModule.enable(this, znCommand)));
        modules.add(module("inventory", () -> InventoryModule.enable(this, znCommand)));
        modules.add(module("clans", () -> ClanModule.enable(this, znCommand)));
        modules.add(module("itemedit", () -> ItemEditModule.enable(this, znCommand)));
        modules.add(module("integrations", () -> IntegrationsModule.enable(this, znCommand)));
        modules.add(module("debug", () -> DebugModule.enable(this, znCommand)));
        BootReport report = new BootReport(getDescription().getVersion(), integrations, modules);
        getLogger().info(translator.get(TranslationKey.BOOT_VERSION, report.version()));
        PaperBoot.send(getServer().getConsoleSender(), BootPrinter.render(report, translator));
    }

    private BootReport.Module module(String name, LongSupplier body) {
        try {
            return new BootReport.Module(name, true, body.getAsLong());
        } catch (Exception e) {
            getLogger().warning(e.toString());
            return new BootReport.Module(name, false, 0);
        }
    }

    private void checkForUpdates() {
        String version = getDescription().getVersion();
        new BukkitRunnable() {
            @Override
            public void run() {
                UpdateChecker.UpdateResult result = UpdateChecker.check(version, "VictorGugug/Zelly-s-Nexus",
                        new GitHubTagSource("VictorGugug/Zelly-s-Nexus"));
                if (result.status() == UpdateChecker.Status.UPDATE_AVAILABLE) {
                    getLogger().info(translator.get(TranslationKey.UPDATE_AVAILABLE, result.latestVersion(), result.url()));
                } else if (result.status() == UpdateChecker.Status.CHECK_FAILED) {
                    getLogger().warning(translator.get(TranslationKey.UPDATE_CHECK_FAILED));
                } else {
                    getLogger().info(translator.get(TranslationKey.UPDATE_UPTODATE, version));
                }
            }
        }.runTaskAsynchronously(this);
    }

    @Override
    public void onDisable() {
        if (tab != null) {
            tab.clear();
        }
        getLogger().info(translator.get(TranslationKey.PLUGIN_DISABLED));
    }

    public Translator translator() {
        return translator;
    }

    public DialogManager dialogs() {
        return dialogs;
    }

    public ZnRootCommand znCommand() {
        return znCommand;
    }

    public void permission(String node, PermissionDefault def) {
        if (getServer().getPluginManager().getPermission(node) == null) {
            getServer().getPluginManager().addPermission(new Permission(node, def));
        }
    }

    private BootReport.Integration integration(String name) {
        Plugin plugin = getServer().getPluginManager().getPlugin(name);
        return new BootReport.Integration(name, plugin != null && plugin.isEnabled());
    }
}
