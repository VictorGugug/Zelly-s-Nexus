/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.clans;

import dev.zellys.nexus.ZellysNexus;
import dev.zellys.nexus.common.store.YamlStore;

public final class ClanModule {
    private ClanModule() {
    }

    public static long enable(ZellysNexus plugin) {
        return enable(plugin, plugin.znCommand());
    }

    public static long enable(ZellysNexus plugin, dev.zellys.nexus.command.ZnRootCommand znCommand) {
        long start = System.nanoTime();
        ClanService service = new ClanService(
                new YamlStore(plugin.getDataFolder().toPath().resolve("clans.yml")));
        plugin.getServer().getPluginManager().registerEvents(new ClanListener(service), plugin);
        ClanSubcommand cmd = new ClanSubcommand(plugin.translator(), service);
        plugin.registerCommand("clan", cmd);
        if (znCommand != null) {
            znCommand.registerCommand("clans", "clan", cmd);
        }
        return (System.nanoTime() - start) / 1_000_000;
    }
}
