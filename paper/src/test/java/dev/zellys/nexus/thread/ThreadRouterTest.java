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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class ThreadRouterTest {
    private final List<String> calls = new ArrayList<>();
    private boolean ownsEntity;
    private boolean globalThread;

    @SuppressWarnings("unchecked")
    private <T> T fake(Class<T> type, String name) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            String call = name + "." + method.getName();
            switch (call) {
                case "server.isOwnedByCurrentRegion" -> {
                    return ownsEntity;
                }
                case "server.isGlobalTickThread" -> {
                    return globalThread;
                }
                case "server.getGlobalRegionScheduler" -> {
                    return fake(GlobalRegionScheduler.class, "global");
                }
                case "server.getRegionScheduler" -> {
                    return fake(RegionScheduler.class, "region");
                }
                case "server.getAsyncScheduler" -> {
                    return fake(AsyncScheduler.class, "async");
                }
                case "plugin.getServer" -> {
                    return fake(Server.class, "server");
                }
                case "player.getScheduler" -> {
                    return fake(EntityScheduler.class, "entity");
                }
                default -> {
                    calls.add(call);
                    for (Object arg : args == null ? new Object[0] : args) {
                        if (arg instanceof Runnable runnable) {
                            runnable.run();
                        } else if (arg instanceof Consumer<?> consumer) {
                            ((Consumer<Object>) consumer).accept(null);
                        }
                    }
                    return null;
                }
            }
        });
    }

    @Test
    void ownedEntityRunsInline() {
        ownsEntity = true;
        ThreadRouter.owner(fake(Plugin.class, "plugin"), fake(Player.class, "player"), () -> calls.add("action"));
        assertEquals(List.of("action"), calls);
    }

    @Test
    void foreignEntityGoesThroughItsScheduler() {
        ThreadRouter.owner(fake(Plugin.class, "plugin"), fake(Player.class, "player"), () -> calls.add("action"));
        assertEquals(List.of("entity.run", "action"), calls);
    }

    @Test
    void consoleOnGlobalThreadRunsInline() {
        globalThread = true;
        ThreadRouter.owner(fake(Plugin.class, "plugin"), fake(ConsoleCommandSender.class, "console"), () -> calls.add("action"));
        assertEquals(List.of("action"), calls);
    }

    @Test
    void consoleOffGlobalThreadGoesThroughGlobalScheduler() {
        ThreadRouter.owner(fake(Plugin.class, "plugin"), fake(ConsoleCommandSender.class, "console"), () -> calls.add("action"));
        assertEquals(List.of("global.execute", "action"), calls);
    }

    @Test
    void laterUsesEntitySchedulerDelay() {
        ThreadRouter.ownerLater(fake(Plugin.class, "plugin"), fake(Player.class, "player"), 20L, () -> calls.add("action"));
        assertEquals(List.of("entity.runDelayed", "action"), calls);
    }

    @Test
    void ownedLocationRunsInline() {
        ownsEntity = true;
        ThreadRouter.region(fake(Plugin.class, "plugin"), new Location(null, 0, 64, 0), () -> calls.add("action"));
        assertEquals(List.of("action"), calls);
    }

    @Test
    void foreignLocationGoesThroughRegionScheduler() {
        ThreadRouter.region(fake(Plugin.class, "plugin"), new Location(null, 0, 64, 0), () -> calls.add("action"));
        assertEquals(List.of("region.execute", "action"), calls);
    }

    @Test
    void asyncUsesAsyncScheduler() {
        ThreadRouter.async(fake(Plugin.class, "plugin"), () -> calls.add("action"));
        assertEquals(List.of("async.runNow", "action"), calls);
    }

    @Test
    void paperIsNotRegionized() {
        assertFalse(ThreadRouter.REGIONIZED);
    }
}
