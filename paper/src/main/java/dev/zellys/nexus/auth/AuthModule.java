/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.auth;

import dev.zellys.nexus.ZellysNexus;
import dev.zellys.nexus.common.store.YamlStore;
import org.bukkit.entity.Player;

public final class AuthModule {
    private AuthModule() {
    }

    public static long enable(ZellysNexus plugin) {
        return enable(plugin, plugin.znCommand());
    }

    public static long enable(ZellysNexus plugin, dev.zellys.nexus.command.ZnRootCommand znCommand) {
        long start = System.nanoTime();
        AuthService service = new AuthService(
                new YamlStore(plugin.getDataFolder().toPath().resolve("auth.yml")));
        plugin.getServer().getPluginManager().registerEvents(new AuthListener(plugin.translator(), service), plugin);
        AuthSubcommand cmd = new AuthSubcommand(plugin.translator(), service, plugin.getServer(), plugin.dialogs());
        plugin.registerCommand("auth", cmd);
        if (znCommand != null) {
            znCommand.registerCommand("auth", "a", cmd);
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    public static String address(Player player) {
        if (player.getAddress() == null || player.getAddress().getAddress() == null) {
            return "";
        }
        return player.getAddress().getAddress().getHostAddress();
    }
}
