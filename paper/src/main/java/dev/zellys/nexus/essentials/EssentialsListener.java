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
import dev.zellys.nexus.common.translation.TranslationKey;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public final class EssentialsListener implements Listener {
    private final ZellysNexus plugin;
    private final EssentialsData data;

    public EssentialsListener(ZellysNexus plugin, EssentialsData data) {
        this.plugin = plugin;
        this.data = data;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (data.muted.contains(player.getUniqueId())) {
            player.sendMessage(plugin.translator().get(TranslationKey.ESS_MUTED_NOTICE));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        Player player = event.getPlayer();
        if (data.jailed.containsKey(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getWorld() != to.getWorld() || from.distanceSquared(to) > 0.01) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        data.backLocations.put(event.getPlayer().getUniqueId(), event.getFrom());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!data.powertoolsEnabled.contains(player.getUniqueId())) {
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        var bindings = data.powertools.get(player.getUniqueId());
        if (bindings != null) {
            String cmd = bindings.get(item.getType());
            if (cmd != null) {
                player.chat("/" + cmd);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (data.vanished.getOrDefault(player.getUniqueId(), false)) {
            player.setInvisible(true);
        }
        if (data.flyMode.getOrDefault(player.getUniqueId(), false)) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        String jail = data.jailed.get(player.getUniqueId());
        if (jail != null) {
            Location cell = data.jails.get(jail);
            if (cell != null) {
                player.teleportAsync(cell);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        data.tpaRequests.values().removeIf(req -> req.sender().equals(player.getUniqueId()) || req.target().equals(player.getUniqueId()));
        data.lastSeen.put(player.getUniqueId(), System.currentTimeMillis());
        data.saveLastSeen();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (data.godMode.getOrDefault(player.getUniqueId(), false)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (data.unlimited.contains(player.getUniqueId())) {
            ItemStack hand = event.getItemInHand();
            // ponytail: Simple working solution - just give them back the item they placed.
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                player.getInventory().addItem(new ItemStack(hand.getType(), 1));
            });
        }
    }
}
