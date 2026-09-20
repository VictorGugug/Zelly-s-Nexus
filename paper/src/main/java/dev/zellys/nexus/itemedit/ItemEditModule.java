/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.itemedit;

import dev.zellys.nexus.ZellysNexus;
import org.bukkit.permissions.PermissionDefault;

public final class ItemEditModule {
    private ItemEditModule() {
    }

    public static long enable(ZellysNexus plugin) {
        return enable(plugin, plugin.znCommand());
    }

    public static long enable(ZellysNexus plugin, dev.zellys.nexus.command.ZnRootCommand znCommand) {
        long start = System.nanoTime();
        ItemStorage storage = new ItemStorage(plugin.getDataFolder().toPath());
        ItemEditSubcommand cmd = new ItemEditSubcommand(plugin.translator(), storage, plugin.dialogs());
        plugin.registerCommand("itemedit", cmd);
        if (znCommand != null) {
            znCommand.registerCommand("itemedit", "ie", cmd);
        }
        plugin.permission("zellysnexus.itemedit", PermissionDefault.OP);
        return (System.nanoTime() - start) / 1_000_000;
    }
}
