/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.inventory;

import dev.zellys.nexus.ZellysNexus;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import dev.zellys.nexus.thread.ThreadRouter;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionDefault;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

final class DeathBackup {
    private DeathBackup() {
    }

    record Snapshot(ItemStack[] contents, ItemStack[] armor, int level) {
    }

    static final Map<UUID, Snapshot> BACKUPS = new HashMap<>();

    static ItemStack[] copy(ItemStack[] stacks) {
        ItemStack[] out = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            out[i] = stacks[i] == null ? null : stacks[i].clone();
        }
        return out;
    }
}

final class DeathListener implements Listener {
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DeathBackup.BACKUPS.put(player.getUniqueId(), new DeathBackup.Snapshot(
                DeathBackup.copy(player.getInventory().getContents()),
                DeathBackup.copy(player.getInventory().getArmorContents()),
                player.getLevel()));
    }
}

final class InventorySubcommand implements BasicCommand {
    private final ZellysNexus plugin;
    private final Translator translator;

    InventorySubcommand(ZellysNexus plugin) {
        this.plugin = plugin;
        this.translator = plugin.translator();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            return;
        }

        if (args.length == 0) {
            player.sendMessage(translator.get(TranslationKey.INV_USAGE));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "see":
                if (!player.hasPermission("zn.inventory.see")) {
                    player.sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                if (args.length != 2) {
                    player.sendMessage(translator.get(TranslationKey.INV_USAGE));
                    return;
                }
                Player seeTarget = player.getServer().getPlayerExact(args[1]);
                if (seeTarget == null) {
                    player.sendMessage(translator.get(TranslationKey.INV_OFFLINE));
                    return;
                }
                player.openInventory(seeTarget.getInventory());
                break;

            case "endersee":
                if (!player.hasPermission("zn.inventory.see")) {
                    player.sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                if (args.length != 2) {
                    player.sendMessage(translator.get(TranslationKey.INV_USAGE));
                    return;
                }
                Player enderSeeTarget = player.getServer().getPlayerExact(args[1]);
                if (enderSeeTarget == null) {
                    player.sendMessage(translator.get(TranslationKey.INV_OFFLINE));
                    return;
                }
                player.openInventory(enderSeeTarget.getEnderChest());
                break;

            case "clear":
                if (!player.hasPermission("zn.inventory.clear")) {
                    player.sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                if (args.length != 2) {
                    player.sendMessage(translator.get(TranslationKey.INV_USAGE));
                    return;
                }
                Player clearTarget = player.getServer().getPlayerExact(args[1]);
                if (clearTarget == null) {
                    player.sendMessage(translator.get(TranslationKey.INV_OFFLINE));
                    return;
                }
                ThreadRouter.owner(plugin, clearTarget, () -> clearTarget.getInventory().clear());
                player.sendMessage(translator.get(TranslationKey.INV_CLEARED, clearTarget.getName()));
                break;

            case "enderclear":
                if (!player.hasPermission("zn.inventory.clear")) {
                    player.sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                if (args.length != 2) {
                    player.sendMessage(translator.get(TranslationKey.INV_USAGE));
                    return;
                }
                Player enderClearTarget = player.getServer().getPlayerExact(args[1]);
                if (enderClearTarget == null) {
                    player.sendMessage(translator.get(TranslationKey.INV_OFFLINE));
                    return;
                }
                ThreadRouter.owner(plugin, enderClearTarget, () -> enderClearTarget.getEnderChest().clear());
                player.sendMessage(translator.get(TranslationKey.INV_ENDER_CLEARED, enderClearTarget.getName()));
                break;

            case "clone":
                if (!player.hasPermission("zn.inventory.clone")) {
                    player.sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                if (args.length != 3) {
                    player.sendMessage(translator.get(TranslationKey.INV_USAGE));
                    return;
                }
                Player cloneSrc = player.getServer().getPlayerExact(args[1]);
                Player cloneDest = player.getServer().getPlayerExact(args[2]);
                if (cloneSrc == null || cloneDest == null) {
                    player.sendMessage(translator.get(TranslationKey.INV_OFFLINE));
                    return;
                }
                ThreadRouter.owner(plugin, cloneSrc, () -> {
                    ItemStack[] contents = DeathBackup.copy(cloneSrc.getInventory().getContents());
                    ThreadRouter.owner(plugin, cloneDest, () -> cloneDest.getInventory().setContents(contents));
                });
                player.sendMessage(translator.get(TranslationKey.INV_CLONED, cloneSrc.getName(), cloneDest.getName()));
                break;

            case "enderclone":
                if (!player.hasPermission("zn.inventory.clone")) {
                    player.sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                if (args.length != 3) {
                    player.sendMessage(translator.get(TranslationKey.INV_USAGE));
                    return;
                }
                Player enderCloneSrc = player.getServer().getPlayerExact(args[1]);
                Player enderCloneDest = player.getServer().getPlayerExact(args[2]);
                if (enderCloneSrc == null || enderCloneDest == null) {
                    player.sendMessage(translator.get(TranslationKey.INV_OFFLINE));
                    return;
                }
                ThreadRouter.owner(plugin, enderCloneSrc, () -> {
                    ItemStack[] contents = DeathBackup.copy(enderCloneSrc.getEnderChest().getContents());
                    ThreadRouter.owner(plugin, enderCloneDest, () -> enderCloneDest.getEnderChest().setContents(contents));
                });
                player.sendMessage(translator.get(TranslationKey.INV_ENDER_CLONED, enderCloneSrc.getName(), enderCloneDest.getName()));
                break;

            case "give":
                if (!player.hasPermission("zn.inventory.give")) {
                    player.sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                if (args.length < 3 || args.length > 4) {
                    player.sendMessage(translator.get(TranslationKey.INV_USAGE));
                    return;
                }
                Player giveTarget = player.getServer().getPlayerExact(args[1]);
                if (giveTarget == null) {
                    player.sendMessage(translator.get(TranslationKey.INV_OFFLINE));
                    return;
                }
                Material mat = Material.matchMaterial(args[2]);
                if (mat == null) {
                    player.sendMessage(translator.get(TranslationKey.ESS_INVALID_MATERIAL, args[2]));
                    return;
                }
                int amount = 1;
                if (args.length == 4) {
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(translator.get(TranslationKey.INV_USAGE));
                        return;
                    }
                }
                ItemStack given = new ItemStack(mat, amount);
                ThreadRouter.owner(plugin, giveTarget, () -> giveTarget.getInventory().addItem(given));
                player.sendMessage(translator.get(TranslationKey.INV_GIVEN, amount + "x " + mat.name(), giveTarget.getName()));
                break;

            case "endergive":
                if (!player.hasPermission("zn.inventory.give")) {
                    player.sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                if (args.length < 3 || args.length > 4) {
                    player.sendMessage(translator.get(TranslationKey.INV_USAGE));
                    return;
                }
                Player enderGiveTarget = player.getServer().getPlayerExact(args[1]);
                if (enderGiveTarget == null) {
                    player.sendMessage(translator.get(TranslationKey.INV_OFFLINE));
                    return;
                }
                Material enderMat = Material.matchMaterial(args[2]);
                if (enderMat == null) {
                    player.sendMessage(translator.get(TranslationKey.ESS_INVALID_MATERIAL, args[2]));
                    return;
                }
                int enderAmount = 1;
                if (args.length == 4) {
                    try {
                        enderAmount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(translator.get(TranslationKey.INV_USAGE));
                        return;
                    }
                }
                ItemStack enderGiven = new ItemStack(enderMat, enderAmount);
                ThreadRouter.owner(plugin, enderGiveTarget, () -> enderGiveTarget.getEnderChest().addItem(enderGiven));
                player.sendMessage(translator.get(TranslationKey.INV_ENDER_GIVEN, enderAmount + "x " + enderMat.name(), enderGiveTarget.getName()));
                break;

            case "rollback":
                if (!player.hasPermission("zn.inventory.rollback")) {
                    player.sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                Player rollTarget = player;
                if (args.length == 2) {
                    rollTarget = player.getServer().getPlayerExact(args[1]);
                    if (rollTarget == null) {
                        player.sendMessage(translator.get(TranslationKey.INV_OFFLINE));
                        return;
                    }
                }
                DeathBackup.Snapshot snapshot = DeathBackup.BACKUPS.get(rollTarget.getUniqueId());
                if (snapshot == null) {
                    player.sendMessage(translator.get(TranslationKey.INV_EMPTY));
                    return;
                }
                Player restored = rollTarget;
                ThreadRouter.owner(plugin, restored, () -> {
                    restored.getInventory().setContents(DeathBackup.copy(snapshot.contents()));
                    restored.getInventory().setArmorContents(DeathBackup.copy(snapshot.armor()));
                    restored.setLevel(snapshot.level());
                });
                player.sendMessage(translator.get(TranslationKey.INV_RESTORED));
                break;

            case "reload":
                if (!player.hasPermission("zn.inventory.admin")) {
                    player.sendMessage(translator.get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                DeathBackup.BACKUPS.keySet().removeIf(id -> player.getServer().getPlayer(id) == null);
                player.sendMessage(translator.get(TranslationKey.INV_RELOADED));
                break;

            default:
                player.sendMessage(translator.get(TranslationKey.CMD_UNKNOWN_SUB, sub));
                break;
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("see", "endersee", "clear", "enderclear", "clone", "enderclone", "give", "endergive", "rollback", "reload");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Arrays.asList("see", "endersee", "clear", "enderclear", "clone", "enderclone", "give", "endergive", "rollback").contains(sub)) {
                return source.getSender().getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Arrays.asList("clone", "enderclone").contains(sub)) {
                return source.getSender().getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
            if (Arrays.asList("give", "endergive").contains(sub)) {
                return Arrays.stream(Material.values())
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .filter(name -> name.startsWith(args[2].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public @Nullable String permission() {
        return "zn.inventory.see"; 
    }
}

public final class InventoryModule {
    private InventoryModule() {
    }

    public static long enable(ZellysNexus plugin) {
        return enable(plugin, plugin.znCommand());
    }

    public static long enable(ZellysNexus plugin, dev.zellys.nexus.command.ZnRootCommand znCommand) {
        long start = System.nanoTime();
        plugin.getServer().getPluginManager().registerEvents(new DeathListener(), plugin);
        InventorySubcommand cmd = new InventorySubcommand(plugin);
        plugin.registerCommand("inventory", cmd);
        if (znCommand != null) {
            znCommand.registerCommand("inventory", "inv", cmd);
        }
        plugin.permission("zn.inventory.see", PermissionDefault.OP);
        plugin.permission("zn.inventory.clear", PermissionDefault.OP);
        plugin.permission("zn.inventory.clone", PermissionDefault.OP);
        plugin.permission("zn.inventory.give", PermissionDefault.OP);
        plugin.permission("zn.inventory.rollback", PermissionDefault.OP);
        plugin.permission("zn.inventory.admin", PermissionDefault.OP);
        return (System.nanoTime() - start) / 1_000_000;
    }
}
