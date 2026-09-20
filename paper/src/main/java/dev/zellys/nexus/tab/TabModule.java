/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.tab;

import dev.zellys.nexus.ZellysNexus;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class TabListener implements Listener {
    private final TabService service;
    private final Set<UUID> disabled;

    TabListener(TabService service, Set<UUID> disabled) {
        this.service = service;
        this.disabled = disabled;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!disabled.contains(event.getPlayer().getUniqueId())) {
            service.apply(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.remove(event.getPlayer());
    }
}

final class TabSubcommand implements BasicCommand {
    private final ZellysNexus plugin;
    private final Translator translator;
    private final TabService service;
    private final Set<UUID> disabled;

    TabSubcommand(ZellysNexus plugin, TabService service, Set<UUID> disabled) {
        this.plugin = plugin;
        this.translator = plugin.translator();
        this.service = service;
        this.disabled = disabled;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        if (args.length == 0) {
            player.sendMessage(translator.get(TranslationKey.TAB_USAGE));
            return;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "info":
                int teams = plugin.getServer().getScoreboardManager().getMainScoreboard().getTeams().size();
                int online = plugin.getServer().getOnlinePlayers().size();
                player.sendMessage(translator.get(TranslationKey.TAB_INFO, String.valueOf(teams), String.valueOf(online)));
                break;
            case "reload":
                service.clear();
                for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
                    if (!disabled.contains(p.getUniqueId())) {
                        service.apply(p);
                    }
                }
                player.sendMessage(translator.get(TranslationKey.TAB_RELOADED));
                break;
            case "toggle":
                if (disabled.contains(player.getUniqueId())) {
                    disabled.remove(player.getUniqueId());
                    service.apply(player);
                    player.sendMessage(translator.get(TranslationKey.TAB_TOGGLE_ON));
                } else {
                    disabled.add(player.getUniqueId());
                    service.remove(player);
                    player.sendMessage(translator.get(TranslationKey.TAB_TOGGLE_OFF));
                }
                break;
            default:
                player.sendMessage(translator.get(TranslationKey.TAB_USAGE));
                break;
        }
    }
}

public final class TabModule {
    private TabModule() {
    }

    public static TabService enable(ZellysNexus plugin) {
        return enable(plugin, plugin.znCommand());
    }

    public static TabService enable(ZellysNexus plugin, dev.zellys.nexus.command.ZnRootCommand znCommand) {
        TabService service = new TabService(plugin, plugin.translator(), plugin.getDescription().getVersion());
        Set<UUID> disabled = new HashSet<>();
        plugin.getServer().getPluginManager().registerEvents(new TabListener(service, disabled), plugin);
        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            if (!disabled.contains(player.getUniqueId())) {
                service.apply(player);
            }
        }
        if (znCommand != null) {
            znCommand.registerCommand("tab", "t", new TabSubcommand(plugin, service, disabled));
        }
        return service;
    }
}
