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
import dev.zellys.nexus.thread.ThreadRouter;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class EssentialsSubcommand implements BasicCommand {
    private final ZellysNexus plugin;
    private final EssentialsData data;

    public EssentialsSubcommand(ZellysNexus plugin, EssentialsData data) {
        this.plugin = plugin;
        this.data = data;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(plugin.translator().get(TranslationKey.CMD_PLAYER_ONLY));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.translator().get(TranslationKey.ESS_USAGE));
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            // Teleportation
            case "spawn" -> {
                Location loc = (Location) data.spawnData.get("spawn");
                if (loc != null) {
                    player.teleportAsync(loc).thenAccept(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            player.sendMessage(plugin.translator().get(TranslationKey.ESS_SPAWN_TELEPORT));
                        }
                    });
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_SPAWN));
                }
            }
            case "setspawn" -> {
                if (!player.hasPermission("zn.essentials.admin")) {
                    player.sendMessage(plugin.translator().get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                data.spawnData.put("spawn", player.getLocation());
                data.saveSpawns();
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_SPAWN_SET));
            }
            case "home" -> {
                String name = args.length > 1 ? args[1] : "home";
                Object raw = data.homesData.get(player.getUniqueId().toString());
                if (raw instanceof java.util.Map<?,?> userHomes) {
                    Object locRaw = userHomes.get(name);
                    if (locRaw instanceof Location loc) {
                        player.teleportAsync(loc).thenAccept(success -> {
                            if (Boolean.TRUE.equals(success)) {
                                player.sendMessage(plugin.translator().get(TranslationKey.ESS_HOME_TELEPORT));
                            }
                        });
                        return;
                    }
                }
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_HOME, name));
            }
            case "sethome" -> {
                String name = args.length > 1 ? args[1] : "home";
                data.homesData.computeIfAbsent(player.getUniqueId().toString(), k -> new java.util.HashMap<>());
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> userHomes = (java.util.Map<String, Object>) data.homesData.get(player.getUniqueId().toString());
                userHomes.put(name, player.getLocation());
                data.saveHomes();
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_HOME_SET, name));
            }
            case "delhome" -> {
                if (args.length < 2) return;
                String name = args[1];
                Object raw = data.homesData.get(player.getUniqueId().toString());
                if (raw instanceof java.util.Map<?,?> userHomes) {
                    userHomes.remove(name);
                    data.saveHomes();
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_HOME_DELETED, name));
                }
            }
            case "homes" -> {
                Object raw = data.homesData.get(player.getUniqueId().toString());
                if (raw instanceof java.util.Map<?,?> userHomes && !userHomes.isEmpty()) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_HOME_LIST, String.join(", ", userHomes.keySet().stream().map(Object::toString).toList())));
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_HOME_NONE));
                }
            }
            case "warp" -> {
                if (args.length < 2) return;
                Object locRaw = data.warpsData.get(args[1]);
                if (locRaw instanceof Location loc) {
                    String warpName = args[1];
                    player.teleportAsync(loc).thenAccept(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            player.sendMessage(plugin.translator().get(TranslationKey.ESS_WARP_TELEPORT, warpName));
                        }
                    });
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_WARP, args[1]));
                }
            }
            case "setwarp" -> {
                if (args.length < 2) return;
                if (!player.hasPermission("zn.essentials.admin")) {
                    player.sendMessage(plugin.translator().get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                data.warpsData.put(args[1], player.getLocation());
                data.saveWarps();
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_WARP_SET, args[1]));
            }
            case "delwarp" -> {
                if (args.length < 2) return;
                if (!player.hasPermission("zn.essentials.admin")) {
                    player.sendMessage(plugin.translator().get(TranslationKey.CMD_NO_PERMISSION));
                    return;
                }
                data.warpsData.remove(args[1]);
                data.saveWarps();
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_WARP_DELETED, args[1]));
            }
            case "warps" -> {
                if (data.warpsData.isEmpty()) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_WARP_NONE));
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_WARP_LIST, String.join(", ", data.warpsData.keySet())));
                }
            }
            case "tpa" -> {
                if (args.length < 2) return;
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                if (!data.tpToggles.getOrDefault(target.getUniqueId(), true)) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_TP_DISABLED, target.getName()));
                    return;
                }
                data.tpaRequests.put(target.getUniqueId(), new EssentialsData.TpaRequest(player.getUniqueId(), target.getUniqueId(), false, System.currentTimeMillis()));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_REQUEST_SENT, target.getName()));
                target.sendMessage(plugin.translator().get(TranslationKey.ESS_REQUEST_RECEIVED, player.getName()));
            }
            case "tpahere" -> {
                if (args.length < 2) return;
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                if (!data.tpToggles.getOrDefault(target.getUniqueId(), true)) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_TP_DISABLED, target.getName()));
                    return;
                }
                data.tpaRequests.put(target.getUniqueId(), new EssentialsData.TpaRequest(player.getUniqueId(), target.getUniqueId(), true, System.currentTimeMillis()));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_REQUEST_SENT, target.getName()));
                target.sendMessage(plugin.translator().get(TranslationKey.ESS_REQUEST_HERE, player.getName()));
            }
            case "tpaccept" -> {
                EssentialsData.TpaRequest req = data.tpaRequests.remove(player.getUniqueId());
                if (req == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_REQUEST));
                    return;
                }
                Player sender = Bukkit.getPlayer(req.sender());
                if (sender != null) {
                    if (req.here()) {
                        player.teleportAsync(sender.getLocation());
                    } else {
                        sender.teleportAsync(player.getLocation());
                    }
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_REQUEST_ACCEPTED));
                    sender.sendMessage(plugin.translator().get(TranslationKey.ESS_REQUEST_ACCEPTED));
                }
            }
            case "tpdeny" -> {
                EssentialsData.TpaRequest req = data.tpaRequests.remove(player.getUniqueId());
                if (req == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_REQUEST));
                    return;
                }
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_REQUEST_DENIED));
                Player sender = Bukkit.getPlayer(req.sender());
                if (sender != null) {
                    sender.sendMessage(plugin.translator().get(TranslationKey.ESS_REQUEST_DENIED));
                }
            }
            
            // Moderation
            case "kick" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                if (args.length < 2) return;
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "-";
                    ThreadRouter.owner(plugin, target, () -> target.kick(net.kyori.adventure.text.Component.text(reason)));
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_KICKED, target.getName(), reason));
                }
            }
            
            // Utilities
            case "heal" -> {
                if (!player.hasPermission("zn.essentials.heal")) return;
                Player target = player;
                if (args.length > 1) {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                        return;
                    }
                }
                Player healed = target;
                ThreadRouter.owner(plugin, healed, () -> {
                    var attr = healed.getAttribute(Attribute.MAX_HEALTH);
                    healed.setHealth(attr != null ? attr.getValue() : 20.0);
                });
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_HEALED, target.getName()));
            }
            case "feed" -> {
                if (!player.hasPermission("zn.essentials.feed")) return;
                Player target = player;
                if (args.length > 1) {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                        return;
                    }
                }
                Player fed = target;
                ThreadRouter.owner(plugin, fed, () -> fed.setFoodLevel(20));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_FED, target.getName()));
            }
            case "fly" -> {
                if (!player.hasPermission("zn.essentials.fly")) return;
                boolean current = data.flyMode.getOrDefault(player.getUniqueId(), false);
                data.flyMode.put(player.getUniqueId(), !current);
                player.setAllowFlight(!current);
                player.sendMessage(plugin.translator().get(!current ? TranslationKey.ESS_FLY_ON : TranslationKey.ESS_FLY_OFF));
            }
            case "god" -> {
                if (!player.hasPermission("zn.essentials.god")) return;
                boolean current = data.godMode.getOrDefault(player.getUniqueId(), false);
                data.godMode.put(player.getUniqueId(), !current);
                player.sendMessage(plugin.translator().get(!current ? TranslationKey.ESS_GOD_ON : TranslationKey.ESS_GOD_OFF));
            }
            case "suicide" -> {
                player.setHealth(0.0);
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_SUICIDE));
            }
            case "workbench" -> {
                if (!player.hasPermission("zn.essentials.workbench")) return;
                player.openWorkbench(null, true);
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_WORKBENCH));
            }
            case "enderchest" -> {
                if (!player.hasPermission("zn.essentials.enderchest")) return;
                player.openInventory(player.getEnderChest());
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_ENDERCHEST_OPENED));
            }
            case "disposal" -> {
                if (!player.hasPermission("zn.essentials.disposal")) return;
                player.openInventory(Bukkit.createInventory(null, 54, net.kyori.adventure.text.Component.text("Disposal")));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_DISPOSAL));
            }
            case "hat" -> {
                if (!player.hasPermission("zn.essentials.hat")) return;
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) return;
                ItemStack helmet = player.getInventory().getHelmet();
                player.getInventory().setHelmet(hand);
                player.getInventory().setItemInMainHand(helmet);
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_HAT_SET));
            }
            case "ping" -> {
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_PING, player.getName(), player.getPing()));
            }
            case "getpos" -> {
                Location l = player.getLocation();
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_GETPOS, player.getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getWorld().getName()));
            }
            case "top" -> {
                Location top = player.getWorld().getHighestBlockAt(player.getLocation()).getLocation();
                top.add(0, 1, 0);
                player.teleportAsync(top);
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_TOP));
            }
            case "back" -> {
                Location back = data.backLocations.get(player.getUniqueId());
                if (back != null) {
                    player.teleportAsync(back);
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_BACK));
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_BACK));
                }
            }
            case "vanish" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                boolean current = data.vanished.getOrDefault(player.getUniqueId(), false);
                data.vanished.put(player.getUniqueId(), !current);
                player.setInvisible(!current);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(player)) {
                        ThreadRouter.owner(plugin, online, () -> {
                            if (!current) {
                                online.hidePlayer(plugin, player);
                            } else {
                                online.showPlayer(plugin, player);
                            }
                        });
                    }
                }
                player.sendMessage(plugin.translator().get(!current ? TranslationKey.ESS_VANISH_ON : TranslationKey.ESS_VANISH_OFF));
            }
            case "gamemode" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                try {
                    org.bukkit.GameMode mode = org.bukkit.GameMode.valueOf(args[1].toUpperCase());
                    player.setGameMode(mode);
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_GAMEMODE_SET, mode.name()));
                } catch (IllegalArgumentException ignored) {
                    player.sendMessage(plugin.translator().get(TranslationKey.CMD_UNKNOWN_SUB, args[1]));
                }
            }
            case "time" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                long ticks = switch (args[1].toLowerCase()) {
                    case "day" -> 1000L;
                    case "noon" -> 6000L;
                    case "night" -> 13000L;
                    case "midnight" -> 18000L;
                    default -> {
                        try {
                            yield Long.parseLong(args[1]);
                        } catch (NumberFormatException e) {
                            yield -1L;
                        }
                    }
                };
                if (ticks < 0) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_INVALID_TIME, args[1]));
                    return;
                }
                org.bukkit.World world = player.getWorld();
                ThreadRouter.global(plugin, () -> world.setTime(ticks));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_TIME_SET, args[1]));
            }
            case "weather" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                boolean storm = args[1].equalsIgnoreCase("storm") || args[1].equalsIgnoreCase("rain");
                boolean thunder = args[1].equalsIgnoreCase("thunder");
                org.bukkit.World world = player.getWorld();
                ThreadRouter.global(plugin, () -> {
                    world.setStorm(storm);
                    world.setThundering(thunder);
                });
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_WEATHER_SET, args[1]));
            }
            case "speed" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                try {
                    float speed = Float.parseFloat(args[1]) / 10.0f;
                    player.setWalkSpeed(Math.max(0.0f, Math.min(1.0f, speed)));
                    player.setFlySpeed(Math.max(0.0f, Math.min(1.0f, speed)));
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_SPEED_SET, args[1]));
                } catch (NumberFormatException ignored) {
                    player.sendMessage(plugin.translator().get(TranslationKey.CMD_UNKNOWN_SUB, args[1]));
                }
            }
            case "tpo" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                player.teleportAsync(target.getLocation());
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_TP_OVERRIDE, target.getName()));
            }
            case "tphere" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                target.teleportAsync(player.getLocation());
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_TP_HERE, target.getName()));
            }
            case "tpall" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(player)) {
                        online.teleportAsync(player.getLocation());
                    }
                }
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_TP_ALL));
            }
            case "tppos" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 4) return;
                try {
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double z = Double.parseDouble(args[3]);
                    player.teleportAsync(new Location(player.getWorld(), x, y, z));
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_TP_POS, x, y, z));
                } catch (NumberFormatException ignored) {
                    player.sendMessage(plugin.translator().get(TranslationKey.CMD_UNKNOWN_SUB, sub));
                }
            }
            case "tpr" -> {
                int x = data.tprMin + (int) (Math.random() * (data.tprMax - data.tprMin));
                int z = data.tprMin + (int) (Math.random() * (data.tprMax - data.tprMin));
                int y = player.getWorld().getHighestBlockYAt(x, z) + 1;
                player.teleportAsync(new Location(player.getWorld(), x, y, z));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_TP_RANDOM));
            }
            case "settpr" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 3) return;
                try {
                    data.tprMin = Integer.parseInt(args[1]);
                    data.tprMax = Integer.parseInt(args[2]);
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_TP_RANDOM_SET, args[1], args[2]));
                } catch (NumberFormatException ignored) {
                    player.sendMessage(plugin.translator().get(TranslationKey.CMD_UNKNOWN_SUB, sub));
                }
            }
            case "tptoggle" -> {
                boolean current = data.tpToggles.getOrDefault(player.getUniqueId(), true);
                data.tpToggles.put(player.getUniqueId(), !current);
                player.sendMessage(plugin.translator().get(!current ? TranslationKey.ESS_TP_TOGGLE_ON : TranslationKey.ESS_TP_TOGGLE_OFF));
            }
            case "jump" -> {
                org.bukkit.block.Block target = player.getTargetBlockExact(32);
                if (target != null) {
                    Location dest = target.getLocation().add(0, 1, 0);
                    player.teleportAsync(dest);
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_JUMP));
                }
            }
            case "bottom" -> {
                Location l = player.getLocation();
                int y = player.getWorld().getMinHeight();
                player.teleportAsync(new Location(player.getWorld(), l.getX(), y + 1, l.getZ()));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_BOTTOM));
            }
            case "nick" -> {
                if (args.length < 2) {
                    data.nicknames.remove(player.getUniqueId());
                    data.saveNicknames();
                    player.displayName(null);
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NICK_RESET, player.getName()));
                    return;
                }
                String nick = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                data.nicknames.put(player.getUniqueId(), nick);
                data.saveNicknames();
                player.displayName(net.kyori.adventure.text.Component.text(nick));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_NICK_SET, player.getName(), nick));
            }
            case "realname" -> {
                if (args.length < 2) return;
                String result = args[1];
                for (java.util.Map.Entry<UUID, String> entry : data.nicknames.entrySet()) {
                    if (entry.getValue().equalsIgnoreCase(args[1])) {
                        Player found = Bukkit.getPlayer(entry.getKey());
                        result = found != null ? found.getName() : entry.getKey().toString();
                    }
                }
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_REALNAME, result));
            }
            case "motd" -> player.sendMessage(plugin.translator().get(TranslationKey.ESS_MOTD, Bukkit.getMotd()));
            case "rules" -> player.sendMessage(plugin.translator().get(TranslationKey.ESS_RULES, plugin.translator().get(TranslationKey.ESS_RULES_TEXT)));
            case "seen" -> {
                if (args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target != null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_SEEN, args[1], "0s"));
                    return;
                }
                Long last = null;
                for (java.util.Map.Entry<UUID, Long> entry : data.lastSeen.entrySet()) {
                    org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.getKey());
                    if (offline.getName() != null && offline.getName().equalsIgnoreCase(args[1])) {
                        last = entry.getValue();
                    }
                }
                if (last == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_SEEN, args[1], formatElapsed(System.currentTimeMillis() - last)));
            }
            case "whois" -> {
                Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : player;
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                String address = target.getAddress() != null && target.getAddress().getAddress() != null
                        ? target.getAddress().getAddress().getHostAddress()
                        : "-";
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_WHOIS, target.getName(), address, target.getUniqueId().toString(), target.getGameMode().name()));
            }
            case "msg" -> {
                if (args.length < 3) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                if (data.msgToggle.getOrDefault(target.getUniqueId(), false)) return;
                String text = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                target.sendMessage(plugin.translator().get(TranslationKey.ESS_MSG_RECEIVED, player.getName(), text));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_MSG_SENT, target.getName(), text));
                data.lastMessager.put(target.getUniqueId(), player.getUniqueId());
                data.lastMessager.put(player.getUniqueId(), target.getUniqueId());
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(player) && !online.equals(target) && data.socialSpy.getOrDefault(online.getUniqueId(), false)) {
                        online.sendMessage(plugin.translator().get(TranslationKey.ESS_MSG_RECEIVED, player.getName() + " -> " + target.getName(), text));
                    }
                }
            }
            case "r" -> {
                if (args.length < 2) return;
                UUID last = data.lastMessager.get(player.getUniqueId());
                Player target = last != null ? Bukkit.getPlayer(last) : null;
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_REPLY));
                    return;
                }
                String text = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                target.sendMessage(plugin.translator().get(TranslationKey.ESS_MSG_RECEIVED, player.getName(), text));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_MSG_SENT, target.getName(), text));
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(player) && !online.equals(target) && data.socialSpy.getOrDefault(online.getUniqueId(), false)) {
                        online.sendMessage(plugin.translator().get(TranslationKey.ESS_MSG_RECEIVED, player.getName() + " -> " + target.getName(), text));
                    }
                }
            }
            case "msgtoggle" -> {
                boolean current = data.msgToggle.getOrDefault(player.getUniqueId(), false);
                data.msgToggle.put(player.getUniqueId(), !current);
                player.sendMessage(plugin.translator().get(!current ? TranslationKey.ESS_MSG_TOGGLE_ON : TranslationKey.ESS_MSG_TOGGLE_OFF));
            }
            case "mail" -> {
                if (args.length < 2) return;
                if (args[1].equalsIgnoreCase("read")) {
                    java.util.List<String> inbox = data.mail.getOrDefault(player.getUniqueId(), java.util.List.of());
                    if (inbox.isEmpty()) {
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_MAIL_NONE));
                    } else {
                        for (String m : inbox) player.sendMessage(plugin.translator().get(TranslationKey.ESS_MAIL_READ, m));
                    }
                } else if (args[1].equalsIgnoreCase("clear")) {
                    data.mail.remove(player.getUniqueId());
                    data.saveMail();
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_MAIL_CLEARED));
                } else if (args[1].equalsIgnoreCase("send") && args.length >= 4) {
                    Player target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                        return;
                    }
                    String text = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                    data.mail.computeIfAbsent(target.getUniqueId(), k -> new java.util.ArrayList<>()).add(player.getName() + ": " + text);
                    data.saveMail();
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_MAIL_SENT, target.getName()));
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_MAIL_USAGE));
                }
            }
            case "mute" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                if (args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) return;
                data.muted.add(target.getUniqueId());
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_MUTED, target.getName()));
            }
            case "unmute" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                if (args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) return;
                data.muted.remove(target.getUniqueId());
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_UNMUTED, target.getName()));
            }
            case "socialspy" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                boolean current = data.socialSpy.getOrDefault(player.getUniqueId(), false);
                data.socialSpy.put(player.getUniqueId(), !current);
                player.sendMessage(plugin.translator().get(!current ? TranslationKey.ESS_SOCIALSPY_ON : TranslationKey.ESS_SOCIALSPY_OFF));
            }
            case "broadcast" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                String text = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                Bukkit.broadcast(net.kyori.adventure.text.Component.text(plugin.translator().get(TranslationKey.ESS_BROADCAST, text)));
            }
            case "helpop" -> {
                if (args.length < 2) return;
                String text = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("zn.essentials.mod")) {
                        online.sendMessage(plugin.translator().get(TranslationKey.ESS_HELPOP, player.getName(), text));
                    }
                }
            }
            case "near" -> {
                java.util.List<String> names = new java.util.ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(player) && online.getWorld().equals(player.getWorld()) && online.getLocation().distance(player.getLocation()) <= 100) {
                        names.add(online.getName());
                    }
                }
                if (names.isEmpty()) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NEAR_NONE));
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NEAR, String.join(", ", names)));
                }
            }
            case "more" -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) return;
                hand.setAmount(hand.getMaxStackSize());
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_MORE));
            }
            case "repair" -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR || !(hand.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable)) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_REPAIR_NOTHING));
                    return;
                }
                damageable.setDamage(0);
                hand.setItemMeta(damageable);
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_REPAIRED));
            }
            case "ext" -> {
                player.setFireTicks(0);
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_EXTINGUISHED, player.getName()));
            }
            case "burn" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                if (args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) return;
                ThreadRouter.owner(plugin, target, () -> target.setFireTicks(100));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_BURNED, target.getName(), 5));
                target.sendMessage(plugin.translator().get(TranslationKey.ESS_BURNED, target.getName(), 5));
            }
            case "lightning" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                Location strike = player.getTargetBlockExact(32) != null ? player.getTargetBlockExact(32).getLocation() : player.getLocation();
                player.getWorld().strikeLightning(strike);
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_LIGHTNING, strike.getBlockX() + "," + strike.getBlockY() + "," + strike.getBlockZ()));
            }
            case "thunder" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                org.bukkit.World world = player.getWorld();
                boolean on = !world.hasStorm();
                ThreadRouter.global(plugin, () -> world.setThundering(on));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_THUNDER, on ? "on" : "off"));
            }
            case "sudo" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 3) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) return;
                String cmd = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                ThreadRouter.owner(plugin, target, () -> target.chat(cmd.startsWith("/") ? cmd : "/" + cmd));
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_SUDO, target.getName(), cmd));
            }
            case "kill" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                if (args.length < 2) {
                    player.setHealth(0.0);
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_KILLED, player.getName()));
                } else {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) return;
                    ThreadRouter.owner(plugin, target, () -> target.setHealth(0.0));
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_KILLED, target.getName()));
                }
            }
            case "kickall" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                int kicked = 0;
                for (Player online : new java.util.ArrayList<>(Bukkit.getOnlinePlayers())) {
                    if (!online.equals(player)) {
                        ThreadRouter.owner(plugin, online, () -> online.kick());
                        kicked++;
                    }
                }
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_KICKED_ALL, kicked));
            }
            case "tpaall" -> {
                java.util.List<String> names = new java.util.ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(player)) {
                        data.tpaRequests.put(online.getUniqueId(), new EssentialsData.TpaRequest(player.getUniqueId(), online.getUniqueId(), false, System.currentTimeMillis()));
                        online.sendMessage(plugin.translator().get(TranslationKey.ESS_REQUEST_RECEIVED, player.getName()));
                        names.add(online.getName());
                    }
                }
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_REQUEST_SENT, String.join(", ", names)));
            }
            case "tpacancel" -> {
                boolean removed = data.tpaRequests.values().removeIf(req -> req.sender().equals(player.getUniqueId()));
                player.sendMessage(plugin.translator().get(removed ? TranslationKey.ESS_REQUEST_CANCELLED : TranslationKey.ESS_NO_REQUEST));
            }
            case "kit" -> {
                if (args.length < 2) {
                    if (data.kits.isEmpty()) {
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_NONE));
                    } else {
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_LIST, String.join(", ", data.kits.values().stream().map(EssentialsData.Kit::name).toList())));
                    }
                    return;
                }
                EssentialsData.Kit kit = data.kits.get(args[1].toLowerCase());
                if (kit == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_NOT_FOUND, args[1]));
                    return;
                }
                if (kit.cooldownSeconds() > 0) {
                    Long last = kit.lastUsed().get(player.getUniqueId());
                    long now = System.currentTimeMillis() / 1000;
                    if (last != null && now - last < kit.cooldownSeconds()) {
                        long left = kit.cooldownSeconds() - (now - last);
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_COOLDOWN, kit.name(), left + "s"));
                        return;
                    }
                    kit.lastUsed().put(player.getUniqueId(), now);
                }
                for (ItemStack stack : kit.items()) {
                    player.getInventory().addItem(stack.clone());
                }
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_RECEIVED, kit.name()));
            }
            case "kits" -> {
                if (data.kits.isEmpty()) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_NONE));
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_LIST, String.join(", ", data.kits.values().stream().map(EssentialsData.Kit::name).toList())));
                }
            }
            case "createkit" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                long cooldown = 0L;
                if (args.length >= 3) {
                    try {
                        cooldown = Long.parseLong(args[2]);
                    } catch (NumberFormatException ignored) {
                        player.sendMessage(plugin.translator().get(TranslationKey.CMD_UNKNOWN_SUB, args[2]));
                        return;
                    }
                }
                java.util.List<ItemStack> items = new java.util.ArrayList<>();
                for (ItemStack stack : player.getInventory().getContents()) {
                    if (stack != null && !stack.getType().isAir()) {
                        items.add(stack.clone());
                    }
                }
                data.kits.put(args[1].toLowerCase(), new EssentialsData.Kit(args[1], cooldown, items.toArray(new ItemStack[0]), new java.util.HashMap<>()));
                data.saveKits();
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_CREATED, args[1]));
            }
            case "delkit" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                if (data.kits.remove(args[1].toLowerCase()) == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_NOT_FOUND, args[1]));
                    return;
                }
                data.saveKits();
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_DELETED, args[1]));
            }
            case "showkit" -> {
                if (args.length < 2) return;
                EssentialsData.Kit kit = data.kits.get(args[1].toLowerCase());
                if (kit == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_NOT_FOUND, args[1]));
                    return;
                }
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_SHOW, kit.name(), kit.items().length));
            }
            case "kitreset" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                if (args.length >= 3) {
                    EssentialsData.Kit kit = data.kits.get(args[2].toLowerCase());
                    if (kit == null) {
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_NOT_FOUND, args[2]));
                        return;
                    }
                    kit.lastUsed().remove(target.getUniqueId());
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_RESET, kit.name(), target.getName()));
                } else {
                    for (EssentialsData.Kit kit : data.kits.values()) {
                        kit.lastUsed().remove(target.getUniqueId());
                    }
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_KIT_RESET, "*", target.getName()));
                }
            }
            case "setjail" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                data.jails.put(args[1].toLowerCase(), player.getLocation());
                data.saveJails();
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_JAIL_SET, args[1]));
            }
            case "deljail" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                if (args.length < 2) return;
                if (data.jails.remove(args[1].toLowerCase()) == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_JAIL, args[1]));
                    return;
                }
                data.saveJails();
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_JAIL_DELETED, args[1]));
            }
            case "jails" -> {
                if (data.jails.isEmpty()) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_JAIL_NONE));
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_JAIL_LIST, String.join(", ", data.jails.keySet())));
                }
            }
            case "jail" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                if (args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                String jailName = args.length >= 3 ? args[2].toLowerCase() : data.jails.keySet().stream().findFirst().orElse(null);
                Location jail = jailName != null ? data.jails.get(jailName) : null;
                if (jail == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_JAIL, jailName != null ? jailName : "-"));
                    return;
                }
                data.jailed.put(target.getUniqueId(), jailName);
                data.saveJailed();
                target.teleportAsync(jail);
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_JAILED, target.getName(), jailName));
            }
            case "unjail" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                if (args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                data.jailed.remove(target.getUniqueId());
                data.saveJailed();
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_UNJAILED, target.getName()));
            }
            case "togglejail" -> {
                if (!player.hasPermission("zn.essentials.mod")) return;
                if (args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                if (data.jailed.containsKey(target.getUniqueId())) {
                    data.jailed.remove(target.getUniqueId());
                    data.saveJailed();
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_UNJAILED, target.getName()));
                    return;
                }
                String jailName = args.length >= 3 ? args[2].toLowerCase() : data.jails.keySet().stream().findFirst().orElse(null);
                Location jail = jailName != null ? data.jails.get(jailName) : null;
                if (jail == null) {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_JAIL, jailName != null ? jailName : "-"));
                    return;
                }
                data.jailed.put(target.getUniqueId(), jailName);
                data.saveJailed();
                target.teleportAsync(jail);
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_JAILED, target.getName(), jailName));
            }
            case "warpinfo" -> {
                if (args.length < 2) return;
                Object locRaw = data.warpsData.get(args[1]);
                if (locRaw instanceof Location loc) {
                    String world = loc.getWorld() != null ? loc.getWorld().getName() : "-";
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_WARP_INFO, args[1], loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + " " + world));
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_WARP, args[1]));
                }
            }
            case "renamehome" -> {
                if (args.length < 3) return;
                Object raw = data.homesData.get(player.getUniqueId().toString());
                if (raw instanceof java.util.Map<?,?> userHomes) {
                    Object loc = userHomes.remove(args[1]);
                    if (loc == null) {
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_HOME, args[1]));
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> homes = (java.util.Map<String, Object>) userHomes;
                    homes.put(args[2], loc);
                    data.saveHomes();
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_HOME_RENAMED, args[1], args[2]));
                } else {
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_NO_HOME, args[1]));
                }
            }
            case "powertool" -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) return;
                if (args.length < 2) return;
                if (args[1].equalsIgnoreCase("clear")) {
                    java.util.Map<Material, String> bindings = data.powertools.get(player.getUniqueId());
                    if (bindings != null) {
                        bindings.remove(hand.getType());
                    }
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_POWERTOOL_CLEARED));
                    return;
                }
                if (args[1].equalsIgnoreCase("toggle")) {
                    if (data.powertoolsEnabled.contains(player.getUniqueId())) {
                        data.powertoolsEnabled.remove(player.getUniqueId());
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_POWERTOOL_OFF));
                    } else {
                        data.powertoolsEnabled.add(player.getUniqueId());
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_POWERTOOL_ON));
                    }
                    return;
                }
                if (args[1].equalsIgnoreCase("list")) {
                    java.util.Map<Material, String> bindings = data.powertools.get(player.getUniqueId());
                    if (bindings == null || bindings.isEmpty()) {
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_POWERTOOL_NONE));
                    } else {
                        java.util.List<String> lines = new java.util.ArrayList<>();
                        for (java.util.Map.Entry<Material, String> entry : bindings.entrySet()) {
                            lines.add(entry.getKey().name() + "=" + entry.getValue());
                        }
                        player.sendMessage(plugin.translator().get(TranslationKey.ESS_POWERTOOL_LIST, String.join(", ", lines)));
                    }
                    return;
                }
                String cmd = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                data.powertools.computeIfAbsent(player.getUniqueId(), k -> new java.util.HashMap<>()).put(hand.getType(), cmd);
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_POWERTOOL_SET, cmd));
            }
            case "jailedplayers" -> {
                java.util.List<String> names = new java.util.ArrayList<>();
                for (UUID id : data.jailed.keySet()) {
                    Player online = Bukkit.getPlayer(id);
                    names.add(online != null ? online.getName() : id.toString());
                }
                player.sendMessage(plugin.translator().get(TranslationKey.ESS_JAIL_PLAYERS, names.isEmpty() ? "-" : String.join(", ", names)));
            }
            case "unlimited" -> {
                if (!player.hasPermission("zn.essentials.admin")) return;
                boolean current = data.unlimited.contains(player.getUniqueId());
                if (current) {
                    data.unlimited.remove(player.getUniqueId());
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_UNLIMITED_OFF, ""));
                } else {
                    data.unlimited.add(player.getUniqueId());
                    player.sendMessage(plugin.translator().get(TranslationKey.ESS_UNLIMITED_ON, ""));
                }
            }

            default -> player.sendMessage(plugin.translator().get(TranslationKey.CMD_UNKNOWN_SUB, sub));
        }
    }

    private static String formatElapsed(long millis) {
        long seconds = Math.max(0L, millis / 1000);
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }
}
