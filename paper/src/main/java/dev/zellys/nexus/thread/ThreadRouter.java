/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 *
 * You may view, study, and modify this file for any noncommercial purpose.
 * You may not use it, or any modified version of it, for commercial purposes,
 * in a closed-source product, or for passive monetization. See LICENSE for
 * the complete terms, including the rules on forks and independent projects.
 */
package dev.zellys.nexus.thread;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class ThreadRouter {
    public static final boolean REGIONIZED = regionized();

    private ThreadRouter() {
    }

    public static void owner(Plugin plugin, CommandSender sender, Runnable action) {
        if (sender instanceof Entity entity) {
            if (plugin.getServer().isOwnedByCurrentRegion(entity)) {
                action.run();
            } else {
                entity.getScheduler().run(plugin, task -> action.run(), null);
            }
        } else {
            global(plugin, action);
        }
    }

    public static void ownerLater(Plugin plugin, Entity entity, long ticks, Runnable action) {
        entity.getScheduler().runDelayed(plugin, task -> action.run(), null, ticks);
    }

    public static void region(Plugin plugin, Location location, Runnable action) {
        if (plugin.getServer().isOwnedByCurrentRegion(location)) {
            action.run();
        } else {
            plugin.getServer().getRegionScheduler().execute(plugin, location, action);
        }
    }

    public static void global(Plugin plugin, Runnable action) {
        if (plugin.getServer().isGlobalTickThread()) {
            action.run();
        } else {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, action);
        }
    }

    public static void async(Plugin plugin, Runnable action) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> action.run());
    }

    private static boolean regionized() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
