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

import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class AuthListener implements Listener {
    private static final Set<String> OPEN_COMMANDS = Set.of("/login", "/l", "/register", "/reg", "/auth");
    private static final Set<String> OPEN_ZN_MODULES = Set.of("help", "auth", "a", "antibot", "ab");

    private final Translator translator;
    private final AuthService service;

    public AuthListener(Translator translator, AuthService service) {
        this.translator = translator;
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ip = AuthModule.address(player);
        if (service.isAuthed(player.getUniqueId(), ip)) {
            player.sendMessage(translator.get(TranslationKey.AUTH_WELCOME_BACK));
        } else if (service.autoLoginIfPremium(player.getUniqueId(), player.getName(), ip)) {
            player.sendMessage(translator.get(TranslationKey.AUTH_LOGGED_IN));
        } else {
            player.sendMessage(translator.get(TranslationKey.AUTH_NEED_LOGIN));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || authed(event.getPlayer())) {
            return;
        }
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!authed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!authed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!authed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!authed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (authed(event.getPlayer())) {
            return;
        }
        String[] parts = event.getMessage().toLowerCase().split(" ");
        if (parts.length > 0 && parts[0].equals("/zn")) {
            if (parts.length == 1 || OPEN_ZN_MODULES.contains(parts[1])) {
                return;
            }
            event.setCancelled(true);
            event.getPlayer().sendMessage(translator.get(TranslationKey.AUTH_NEED_LOGIN));
            return;
        }
        if (!OPEN_COMMANDS.contains(parts[0])) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(translator.get(TranslationKey.AUTH_NEED_LOGIN));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!authed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && !authed(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && !authed(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && !authed(player)) {
            event.setCancelled(true);
        }
    }

    private boolean authed(Player player) {
        return service.isAuthed(player.getUniqueId(), AuthModule.address(player));
    }
}
