/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.essentials;

import dev.zellys.nexus.ZellysNexus;
import dev.zellys.nexus.command.ZnRootCommand;
import org.bukkit.permissions.PermissionDefault;

public final class EssentialsModule {
    private EssentialsModule() {
    }

    public static long enable(ZellysNexus plugin, ZnRootCommand znCommand) {
        long start = System.nanoTime();
        
        EssentialsData data = new EssentialsData(plugin);
        
        plugin.getServer().getPluginManager().registerEvents(new EssentialsListener(plugin, data), plugin);
        
        EssentialsSubcommand command = new EssentialsSubcommand(plugin, data);
        znCommand.registerCommand("essentials", "ess", command);

        plugin.permission("zn.essentials.admin", PermissionDefault.OP);
        plugin.permission("zn.essentials.mod", PermissionDefault.OP);
        plugin.permission("zn.essentials.heal", PermissionDefault.OP);
        plugin.permission("zn.essentials.feed", PermissionDefault.OP);
        plugin.permission("zn.essentials.fly", PermissionDefault.OP);
        plugin.permission("zn.essentials.god", PermissionDefault.OP);
        plugin.permission("zn.essentials.workbench", PermissionDefault.OP);
        plugin.permission("zn.essentials.enderchest", PermissionDefault.OP);
        plugin.permission("zn.essentials.disposal", PermissionDefault.OP);
        plugin.permission("zn.essentials.hat", PermissionDefault.OP);
        
        return (System.nanoTime() - start) / 1_000_000;
    }
}
