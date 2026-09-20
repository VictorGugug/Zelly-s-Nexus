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

import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ClanSubcommand implements BasicCommand {
    private final Translator translator;
    private final ClanService service;

    public ClanSubcommand(Translator translator, ClanService service) {
        this.translator = translator;
        this.service = service;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            return;
        }

        if (args.length == 0) {
            player.sendMessage(translator.get(TranslationKey.CLAN_USAGE));
            return;
        }

        String sub = args[0].toLowerCase();
        UUID uuid = player.getUniqueId();

        switch (sub) {
            case "create": {
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_USAGE));
                    return;
                }
                ClanService.ClanResult res = service.create(uuid, args[1]);
                if (res == ClanService.ClanResult.OK) player.sendMessage(translator.get(TranslationKey.CLAN_CREATED, args[1]));
                else if (res == ClanService.ClanResult.EXISTS) player.sendMessage(translator.get(TranslationKey.CLAN_EXISTS, args[1]));
                else if (res == ClanService.ClanResult.ALREADY) player.sendMessage(translator.get(TranslationKey.CLAN_ALREADY));
                else if (res == ClanService.ClanResult.BANNED) player.sendMessage(translator.get(TranslationKey.CLAN_BANNED, player.getName()));
                else player.sendMessage(translator.get(TranslationKey.CLAN_UNKNOWN));
                break;
            }
            case "disband": {
                if (service.disband(uuid) == ClanService.ClanResult.OK) player.sendMessage(translator.get(TranslationKey.CLAN_DISBANDED));
                else player.sendMessage(translator.get(TranslationKey.CLAN_UNKNOWN));
                break;
            }
            case "invite": {
                if (args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(translator.get(TranslationKey.ESS_PLAYER_OFFLINE));
                    return;
                }
                if (service.invite(uuid, target.getUniqueId()) == ClanService.ClanResult.OK) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_INVITED, target.getName()));
                    ClanService.Clan own = service.clanOf(uuid);
                    target.sendMessage(translator.get(TranslationKey.CLAN_INVITE_RECEIVED, player.getName(), own != null ? own.name : "-"));
                } else player.sendMessage(translator.get(TranslationKey.CLAN_UNKNOWN));
                break;
            }
            case "accept": {
                if (service.accept(uuid) == ClanService.ClanResult.OK) {
                    ClanService.Clan joined = service.clanOf(uuid);
                    player.sendMessage(translator.get(TranslationKey.CLAN_JOINED, joined != null ? joined.name : "-"));
                } else player.sendMessage(translator.get(TranslationKey.CLAN_NO_INVITE));
                break;
            }
            case "kick": {
                if (args.length < 2) return;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) return;
                if (service.kick(uuid, target.getUniqueId()) == ClanService.ClanResult.OK) player.sendMessage(translator.get(TranslationKey.CLAN_KICKED, target.getName()));
                else player.sendMessage(translator.get(TranslationKey.CLAN_UNKNOWN));
                break;
            }
            case "leave": {
                if (service.leave(uuid) == ClanService.ClanResult.OK) player.sendMessage(translator.get(TranslationKey.CLAN_LEFT));
                else player.sendMessage(translator.get(TranslationKey.CLAN_UNKNOWN));
                break;
            }
            case "ally": {
                if (args.length < 3) return;
                if (args[1].equalsIgnoreCase("add") && service.ally(uuid, args[2]) == ClanService.ClanResult.OK) player.sendMessage(translator.get(TranslationKey.CLAN_ALLY_ADDED, args[2]));
                else if (args[1].equalsIgnoreCase("remove") && service.unally(uuid, args[2]) == ClanService.ClanResult.OK) player.sendMessage(translator.get(TranslationKey.CLAN_ALLY_REMOVED, args[2]));
                break;
            }
            case "rival": {
                if (args.length < 3) return;
                if (args[1].equalsIgnoreCase("add") && service.rival(uuid, args[2]) == ClanService.ClanResult.OK) player.sendMessage(translator.get(TranslationKey.CLAN_RIVAL_ADDED, args[2]));
                else if (args[1].equalsIgnoreCase("remove") && service.unrival(uuid, args[2]) == ClanService.ClanResult.OK) player.sendMessage(translator.get(TranslationKey.CLAN_RIVAL_REMOVED, args[2]));
                break;
            }
            case "war": {
                if (args.length < 3) return;
                boolean start = args[1].equalsIgnoreCase("start");
                ClanService.ClanResult r = service.war(uuid, args[2], start);
                if (r == ClanService.ClanResult.WAR_STARTED) player.sendMessage(translator.get(TranslationKey.CLAN_WAR_STARTED, args[2]));
                else if (r == ClanService.ClanResult.WAR_ENDED) player.sendMessage(translator.get(TranslationKey.CLAN_WAR_ENDED, args[2]));
                else player.sendMessage(translator.get(TranslationKey.CLAN_UNKNOWN));
                break;
            }
            case "rank": {
                if (args.length < 2) return;
                if (args[1].equalsIgnoreCase("create") && args.length == 3) {
                    if (service.createRank(uuid, args[2]) == ClanService.ClanResult.RANK_CREATED) player.sendMessage(translator.get(TranslationKey.CLAN_RANK_CREATED, args[2]));
                } else if (args[1].equalsIgnoreCase("delete") && args.length == 3) {
                    if (service.deleteRank(uuid, args[2]) == ClanService.ClanResult.RANK_DELETED) player.sendMessage(translator.get(TranslationKey.CLAN_RANK_DELETED, args[2]));
                } else if (args[1].equalsIgnoreCase("set") && args.length == 4) {
                    Player t = Bukkit.getPlayerExact(args[2]);
                    if (t != null && service.setRank(uuid, t.getUniqueId(), args[3]) == ClanService.ClanResult.RANK_SET) player.sendMessage(translator.get(TranslationKey.CLAN_RANK_SET, t.getName(), args[3]));
                } else if (args[1].equalsIgnoreCase("list")) {
                    List<String> ranks = service.listRanks(uuid);
                    if (ranks != null) player.sendMessage(translator.get(TranslationKey.CLAN_RANK_LIST, String.join(", ", ranks)));
                }
                break;
            }
            case "home": {
                org.bukkit.Location h = service.getHome(uuid);
                if (h != null) {
                    player.teleportAsync(h);
                    player.sendMessage(translator.get(TranslationKey.CLAN_HOME_TELEPORT));
                } else player.sendMessage(translator.get(TranslationKey.CLAN_NO_HOME));
                break;
            }
            case "home-set": {
                if (service.setHome(uuid, player.getLocation()) == ClanService.ClanResult.HOME_SET) player.sendMessage(translator.get(TranslationKey.CLAN_HOME_SET));
                else player.sendMessage(translator.get(TranslationKey.CLAN_UNKNOWN));
                break;
            }
            case "description": {
                if (args.length < 2) return;
                String desc = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (service.setDescription(uuid, desc) == ClanService.ClanResult.DESC_SET) player.sendMessage(translator.get(TranslationKey.CLAN_DESC_SET));
                break;
            }
            case "verify": {
                if (args.length < 2 || !player.hasPermission("zn.admin")) return;
                if (service.verify(args[1]) == ClanService.ClanResult.VERIFIED) player.sendMessage(translator.get(TranslationKey.CLAN_VERIFIED, args[1]));
                break;
            }
            case "ban": {
                if (args.length < 2 || !player.hasPermission("zn.admin")) return;
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t != null && service.banPlayer(t.getUniqueId()) == ClanService.ClanResult.BANNED) player.sendMessage(translator.get(TranslationKey.CLAN_BANNED, t.getName()));
                break;
            }
            case "unban": {
                if (args.length < 2 || !player.hasPermission("zn.admin")) return;
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t != null && service.unbanPlayer(t.getUniqueId()) == ClanService.ClanResult.UNBANNED) player.sendMessage(translator.get(TranslationKey.CLAN_UNBANNED, t.getName()));
                break;
            }
            case "clanff": {
                if (args.length < 2) return;
                boolean on = args[1].equalsIgnoreCase("on");
                if (service.setFriendlyFire(uuid, on) == ClanService.ClanResult.FF_TOGGLED) {
                    player.sendMessage(translator.get(on ? TranslationKey.CLAN_CLANFF_ON : TranslationKey.CLAN_CLANFF_OFF));
                }
                break;
            }
            case "ff": {
                if (args.length < 2) return;
                boolean on = args[1].equalsIgnoreCase("on");
                service.setPersonalFf(uuid, on);
                player.sendMessage(translator.get(on ? TranslationKey.CLAN_FF_ON : TranslationKey.CLAN_FF_OFF));
                break;
            }
            case "bb": {
                if (args.length > 1) {
                    if (service.postBulletin(uuid, String.join(" ", Arrays.copyOfRange(args, 1, args.length))) == ClanService.ClanResult.BB_POSTED) {
                        player.sendMessage(translator.get(TranslationKey.CLAN_BB_POSTED));
                    }
                } else {
                    List<String> bbs = service.getBulletins(uuid);
                    if (bbs.isEmpty()) player.sendMessage(translator.get(TranslationKey.CLAN_BB_EMPTY));
                    else bbs.forEach(player::sendMessage);
                }
                break;
            }
            case "stats": {
                if (args.length < 2) return;
                ClanService.Clan clan = service.listClans().stream().filter(c -> c.name.equalsIgnoreCase(args[1])).findFirst().orElse(null);
                if (clan == null) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_UNKNOWN));
                    break;
                }
                int kills = clan.kills.values().stream().mapToInt(Integer::intValue).sum();
                int deaths = clan.deaths.values().stream().mapToInt(Integer::intValue).sum();
                String kdr = deaths == 0 ? String.valueOf(kills) : String.format(java.util.Locale.ROOT, "%.2f", (double) kills / deaths);
                player.sendMessage(translator.get(TranslationKey.CLAN_STATS, clan.name, kdr, kills, deaths, clan.warsWon));
                break;
            }
            case "roster": {
                if (args.length < 2) return;
                String ros = service.getRoster(args[1]);
                if (ros != null) player.sendMessage(translator.get(TranslationKey.CLAN_ROSTER, args[1], ros));
                break;
            }
            case "list": {
                List<ClanService.Clan> clns = service.listClans();
                if (clns.isEmpty()) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_LIST_EMPTY));
                } else {
                    player.sendMessage(translator.get(TranslationKey.CLAN_LIST_HEADER));
                    for (ClanService.Clan c : clns) player.sendMessage(translator.get(TranslationKey.CLAN_LIST_ENTRY, c.name, c.verified ? "v" : "-", c.members.size()));
                }
                break;
            }
            case "lookup": {
                if (args.length < 2) return;
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t != null) {
                    String l = service.lookupPlayer(t.getUniqueId());
                    if (l != null) player.sendMessage(translator.get(TranslationKey.CLAN_LOOKUP, l));
                }
                break;
            }
            case "regroup": {
                ClanService.Clan clan = service.clanOf(uuid);
                if (clan == null) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_NO_CLAN));
                    break;
                }
                int gathered = 0;
                for (String id : clan.members) {
                    try {
                        Player mate = Bukkit.getPlayer(UUID.fromString(id));
                        if (mate != null && !mate.equals(player)) {
                            mate.teleportAsync(player.getLocation());
                            gathered++;
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                player.sendMessage(translator.get(TranslationKey.CLAN_REGROUP, gathered));
                break;
            }
            case "trust": {
                if (args.length < 2) return;
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t != null && service.trust(uuid, t.getUniqueId()) == ClanService.ClanResult.TRUSTED) player.sendMessage(translator.get(TranslationKey.CLAN_TRUSTED, t.getName()));
                break;
            }
            case "untrust": {
                if (args.length < 2) return;
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t != null && service.untrust(uuid, t.getUniqueId()) == ClanService.ClanResult.UNTRUSTED) player.sendMessage(translator.get(TranslationKey.CLAN_UNTRUSTED, t.getName()));
                break;
            }
            case "mute": {
                if (args.length < 2) return;
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t != null && service.muteMember(uuid, t.getUniqueId()) == ClanService.ClanResult.MEMBER_MUTED) player.sendMessage(translator.get(TranslationKey.CLAN_MEMBER_MUTED, t.getName()));
                break;
            }
            case "setbanner": {
                if (args.length < 2) return;
                if (service.setBanner(uuid, args[1]) == ClanService.ClanResult.BANNER_SET) player.sendMessage(translator.get(TranslationKey.CLAN_BANNER_SET));
                break;
            }
            case "kills": {
                player.sendMessage(translator.get(TranslationKey.CLAN_KILLS, service.getKills(uuid).toString()));
                break;
            }
            case "mostkilled": {
                player.sendMessage(translator.get(TranslationKey.CLAN_MOST_KILLED, service.getMostKilled(uuid)));
                break;
            }
            case "rename": {
                if (args.length < 2) return;
                if (service.rename(uuid, args[1]) == ClanService.ClanResult.RENAMED) player.sendMessage(translator.get(TranslationKey.CLAN_RENAMED, args[1]));
                break;
            }
            case "locale": {
                if (args.length < 2) return;
                if (service.setLocale(uuid, args[1]) == ClanService.ClanResult.LOCALE_SET) player.sendMessage(translator.get(TranslationKey.CLAN_LOCALE_SET, args[1]));
                break;
            }
            case "help": {
                player.sendMessage(translator.get(TranslationKey.CLAN_HELP));
                break;
            }
            case "globalff": {
                if (!player.hasPermission("zn.admin")) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_UNKNOWN));
                    break;
                }
                if (args.length < 2) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_USAGE));
                    break;
                }
                boolean on = args[1].equalsIgnoreCase("on");
                service.setGlobalFf(on);
                player.sendMessage(translator.get(on ? TranslationKey.CLAN_GLOBALFF_ON : TranslationKey.CLAN_GLOBALFF_OFF));
                break;
            }
            case "coords": {
                ClanService.Clan clan = service.clanOf(uuid);
                if (clan == null) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_NO_CLAN));
                    break;
                }
                List<String> lines = new ArrayList<>();
                for (String id : clan.members) {
                    try {
                        Player mate = Bukkit.getPlayer(UUID.fromString(id));
                        if (mate != null && mate.isOnline()) {
                            org.bukkit.Location l = mate.getLocation();
                            lines.add(mate.getName() + " " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                player.sendMessage(translator.get(TranslationKey.CLAN_COORDS, lines.isEmpty() ? "-" : String.join("; ", lines)));
                break;
            }
            case "vitals": {
                ClanService.Clan clan = service.clanOf(uuid);
                if (clan == null) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_NO_CLAN));
                    break;
                }
                List<String> lines = new ArrayList<>();
                for (String id : clan.members) {
                    try {
                        Player mate = Bukkit.getPlayer(UUID.fromString(id));
                        if (mate != null && mate.isOnline()) {
                            lines.add(mate.getName() + " " + Math.round(mate.getHealth()) + "hp food:" + mate.getFoodLevel());
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                player.sendMessage(translator.get(TranslationKey.CLAN_VITALS, lines.isEmpty() ? "-" : String.join("; ", lines)));
                break;
            }
            case "rivalries": {
                ClanService.Clan clan = service.clanOf(uuid);
                if (clan == null) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_NO_CLAN));
                    break;
                }
                player.sendMessage(translator.get(TranslationKey.CLAN_RIVALRIES, clan.rivals.isEmpty() ? "-" : String.join(", ", clan.rivals)));
                break;
            }
            case "profile": {
                ClanService.Clan clan = service.clanOf(uuid);
                String name = args.length > 1 ? args[1] : (clan != null ? clan.name : null);
                ClanService.Clan target = clan;
                if (args.length > 1) {
                    target = service.listClans().stream().filter(c -> c.name.equalsIgnoreCase(args[1])).findFirst().orElse(null);
                }
                if (target == null) {
                    player.sendMessage(translator.get(TranslationKey.CLAN_NO_CLAN));
                    break;
                }
                player.sendMessage(translator.get(TranslationKey.CLAN_PROFILE, target.name + " members:" + target.members.size() + " verified:" + target.verified + " desc:" + target.description));
                break;
            }
            default:
                player.sendMessage(translator.get(TranslationKey.CLAN_USAGE));
                break;
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length <= 1) {
            return Arrays.asList("create", "disband", "invite", "accept", "kick", "leave", "ally", "rival", "war", "rank", "home", "home-set", "description", "verify", "ban", "unban", "ff", "clanff", "globalff", "bb", "stats", "roster", "list", "lookup", "regroup", "trust", "untrust", "mute", "setbanner", "kills", "mostkilled", "coords", "vitals", "rivalries", "profile", "rename", "locale", "help").stream().filter(s -> s.startsWith(args.length == 1 ? args[0].toLowerCase() : "")).collect(Collectors.toList());
        }
        return List.of();
    }
}
