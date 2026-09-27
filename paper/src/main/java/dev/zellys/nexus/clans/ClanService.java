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

import dev.zellys.nexus.common.store.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ClanService {
    static final long INVITE_SECONDS = 120;
    static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,12}$");

    public enum ClanResult {
        OK, BAD_NAME, EXISTS, ALREADY, NO_CLAN, NOT_MEMBER, NOT_OWNER, NO_INVITE,
        UNKNOWN_CLAN, SELF, TARGET_HAS_CLAN, OWNER_LEAVE,
        WAR_STARTED, WAR_ENDED, RANK_SET, RANK_CREATED, RANK_DELETED,
        HOME_SET, DESC_SET, VERIFIED, BANNED, UNBANNED, FF_TOGGLED,
        BB_POSTED, TRUSTED, UNTRUSTED, MEMBER_MUTED, BANNER_SET,
        RENAMED, LOCALE_SET, NOT_LEADER, ALREADY_WAR, NO_WAR
    }

    public static final class Clan {
        public String name;
        public String owner;
        public String description = "";
        public Location home = null;
        public boolean verified = false;
        public boolean friendlyFire = false;
        public String banner = null;
        public String locale = "en";
        public int warsWon = 0;

        public final Set<String> members = ConcurrentHashMap.newKeySet();
        public final Set<String> allies = ConcurrentHashMap.newKeySet();
        public final Set<String> rivals = ConcurrentHashMap.newKeySet();
        public final Set<String> wars = ConcurrentHashMap.newKeySet();
        public final Set<String> trusted = ConcurrentHashMap.newKeySet();
        public final Set<String> muted = ConcurrentHashMap.newKeySet();

        public final Map<String, String> ranks = new ConcurrentHashMap<>();
        public final Map<String, String> memberRanks = new ConcurrentHashMap<>();
        public final Map<String, Integer> kills = new ConcurrentHashMap<>();
        public final Map<String, Integer> deaths = new ConcurrentHashMap<>();
        public final List<String> bulletinBoard = new CopyOnWriteArrayList<>();

        Clan(String name, String owner) {
            this.name = name;
            this.owner = owner;
        }
    }

    static final class Invite {
        final String clan;
        final long expiry;
        Invite(String clan, long expiry) {
            this.clan = clan;
            this.expiry = expiry;
        }
    }

    private final YamlStore store;
    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();
    private final Set<String> banned = new HashSet<>();
    private final Set<String> personalFfOff = new HashSet<>();
    private boolean globalFf = false;

    @SuppressWarnings("unchecked")
    public ClanService(YamlStore store) {
        this.store = store;
        Map<String, Object> root = store.load();
        if (root.get("banned") instanceof Iterable<?> iter) {
            for (Object o : iter) {
                if (o instanceof String str) banned.add(str);
            }
        }
        if (root.get("personalFfOff") instanceof Iterable<?> iter) {
            for (Object o : iter) {
                if (o instanceof String str) personalFfOff.add(str);
            }
        }
        if (root.get("globalFf") instanceof Boolean flag) {
            globalFf = flag;
        }
        if (root.get("clans") instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) root.get("clans")).entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof Map) {
                    Clan clan = new Clan((String) entry.getKey(), "");
                    Map<String, Object> data = (Map<String, Object>) entry.getValue();
                    if (data.get("owner") instanceof String str) clan.owner = str;
                    if (data.get("description") instanceof String str) clan.description = str;
                    if (data.get("verified") instanceof Boolean b) clan.verified = b;
                    if (data.get("friendlyFire") instanceof Boolean b) clan.friendlyFire = b;
                    if (data.get("banner") instanceof String str) clan.banner = str;
                    if (data.get("locale") instanceof String str) clan.locale = str;
                    if (data.get("warsWon") instanceof Number n) clan.warsWon = n.intValue();

                    if (data.get("home") instanceof String str) clan.home = deserializeLoc(str);

                    strings(data.get("members"), clan.members);
                    strings(data.get("allies"), clan.allies);
                    strings(data.get("rivals"), clan.rivals);
                    strings(data.get("wars"), clan.wars);
                    strings(data.get("trusted"), clan.trusted);
                    strings(data.get("muted"), clan.muted);

                    if (data.get("ranks") instanceof Map<?, ?> map) {
                        for (Map.Entry<?, ?> e : map.entrySet()) {
                            if (e.getKey() instanceof String k && e.getValue() instanceof String v) clan.ranks.put(k, v);
                        }
                    }
                    if (data.get("memberRanks") instanceof Map<?, ?> map) {
                        for (Map.Entry<?, ?> e : map.entrySet()) {
                            if (e.getKey() instanceof String k && e.getValue() instanceof String v) clan.memberRanks.put(k, v);
                        }
                    }
                    if (data.get("kills") instanceof Map<?, ?> map) {
                        for (Map.Entry<?, ?> e : map.entrySet()) {
                            if (e.getKey() instanceof String k && e.getValue() instanceof Number v) clan.kills.put(k, v.intValue());
                        }
                    }
                    if (data.get("deaths") instanceof Map<?, ?> map) {
                        for (Map.Entry<?, ?> e : map.entrySet()) {
                            if (e.getKey() instanceof String k && e.getValue() instanceof Number v) clan.deaths.put(k, v.intValue());
                        }
                    }
                    if (data.get("bulletinBoard") instanceof Iterable<?> iter) {
                        for (Object o : iter) {
                            if (o instanceof String str) clan.bulletinBoard.add(str);
                        }
                    }

                    clans.put(clan.name.toLowerCase(), clan);
                }
            }
        }
    }

    private String serializeLoc(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private Location deserializeLoc(String str) {
        if (str == null || str.isEmpty()) return null;
        String[] parts = str.split(",");
        if (parts.length != 6) return null;
        try {
            return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
        } catch (Exception e) {
            return null;
        }
    }

    private void strings(Object raw, Set<String> out) {
        if (raw instanceof Iterable) {
            for (Object entry : (Iterable<?>) raw) {
                if (entry instanceof String str) {
                    out.add(str);
                }
            }
        }
    }

    private void persist() {
        Map<String, Object> root = new HashMap<>();
        root.put("banned", new ArrayList<>(banned));
        root.put("personalFfOff", new ArrayList<>(personalFfOff));
        root.put("globalFf", globalFf);

        Map<String, Object> data = new HashMap<>();
        for (Clan clan : clans.values()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("owner", clan.owner);
            entry.put("description", clan.description);
            entry.put("verified", clan.verified);
            entry.put("friendlyFire", clan.friendlyFire);
            entry.put("banner", clan.banner);
            entry.put("locale", clan.locale);
            entry.put("warsWon", clan.warsWon);
            entry.put("home", serializeLoc(clan.home));

            entry.put("members", new ArrayList<>(clan.members));
            entry.put("allies", new ArrayList<>(clan.allies));
            entry.put("rivals", new ArrayList<>(clan.rivals));
            entry.put("wars", new ArrayList<>(clan.wars));
            entry.put("trusted", new ArrayList<>(clan.trusted));
            entry.put("muted", new ArrayList<>(clan.muted));

            entry.put("ranks", new HashMap<>(clan.ranks));
            entry.put("memberRanks", new HashMap<>(clan.memberRanks));
            entry.put("kills", new HashMap<>(clan.kills));
            entry.put("deaths", new HashMap<>(clan.deaths));
            entry.put("bulletinBoard", new ArrayList<>(clan.bulletinBoard));

            data.put(clan.name, entry);
        }
        root.put("clans", data);
        store.save(root);
    }

    public synchronized ClanResult create(UUID owner, String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) return ClanResult.BAD_NAME;
        if (clans.containsKey(name.toLowerCase())) return ClanResult.EXISTS;
        if (clanOf(owner) != null) return ClanResult.ALREADY;
        if (banned.contains(owner.toString())) return ClanResult.BANNED;
        Clan clan = new Clan(name, owner.toString());
        clan.members.add(owner.toString());
        clans.put(name.toLowerCase(), clan);
        persist();
        return ClanResult.OK;
    }

    public synchronized ClanResult invite(UUID owner, UUID target) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        if (owner.equals(target)) return ClanResult.SELF;
        if (clanOf(target) != null) return ClanResult.TARGET_HAS_CLAN;
        if (banned.contains(target.toString())) return ClanResult.BANNED;
        invites.put(target, new Invite(clan.name, Instant.now().getEpochSecond() + INVITE_SECONDS));
        return ClanResult.OK;
    }

    public synchronized ClanResult accept(UUID target) {
        Invite invite = invites.get(target);
        if (invite == null || invite.expiry < Instant.now().getEpochSecond()) {
            invites.remove(target);
            return ClanResult.NO_INVITE;
        }
        if (clanOf(target) != null) {
            invites.remove(target);
            return ClanResult.ALREADY;
        }
        Clan clan = clans.get(invite.clan.toLowerCase());
        if (clan == null) {
            invites.remove(target);
            return ClanResult.UNKNOWN_CLAN;
        }
        clan.members.add(target.toString());
        invites.remove(target);
        persist();
        return ClanResult.OK;
    }

    public synchronized ClanResult kick(UUID owner, UUID target) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        if (owner.equals(target) || !clan.members.contains(target.toString())) return ClanResult.NOT_MEMBER;
        clan.members.remove(target.toString());
        persist();
        return ClanResult.OK;
    }

    public synchronized ClanResult leave(UUID member) {
        Clan clan = clanOf(member);
        if (clan == null) return ClanResult.NO_CLAN;
        if (clan.owner.equals(member.toString())) return ClanResult.OWNER_LEAVE;
        clan.members.remove(member.toString());
        persist();
        return ClanResult.OK;
    }

    public synchronized ClanResult disband(UUID owner) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!clan.owner.equals(owner.toString())) return ClanResult.NOT_OWNER;
        clans.remove(clan.name.toLowerCase());
        invites.entrySet().removeIf(entry -> entry.getValue().clan.equalsIgnoreCase(clan.name));
        persist();
        return ClanResult.OK;
    }

    public synchronized ClanResult ally(UUID owner, String name) { return relate(owner, name, true); }
    public synchronized ClanResult rival(UUID owner, String name) { return relate(owner, name, false); }

    private ClanResult relate(UUID owner, String name, boolean ally) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        Clan other = clans.get(name.toLowerCase());
        if (other == null) return ClanResult.UNKNOWN_CLAN;
        if (other == clan) return ClanResult.SELF;
        if (ally) {
            clan.allies.add(other.name);
            clan.rivals.remove(other.name);
        } else {
            clan.rivals.add(other.name);
            clan.allies.remove(other.name);
        }
        persist();
        return ClanResult.OK;
    }

    public synchronized ClanResult unally(UUID owner, String name) {
        Clan clan = clanOf(owner);
        if (clan != null && isLeader(clan, owner)) {
            Clan other = clans.get(name.toLowerCase());
            if (other != null) clan.allies.remove(other.name);
            persist();
            return ClanResult.OK;
        }
        return ClanResult.NOT_LEADER;
    }

    public synchronized ClanResult unrival(UUID owner, String name) {
        Clan clan = clanOf(owner);
        if (clan != null && isLeader(clan, owner)) {
            Clan other = clans.get(name.toLowerCase());
            if (other != null) clan.rivals.remove(other.name);
            persist();
            return ClanResult.OK;
        }
        return ClanResult.NOT_LEADER;
    }


    public synchronized Clan clanOf(UUID member) {
        for (Clan clan : clans.values()) {
            if (clan.members.contains(member.toString())) return clan;
        }
        return null;
    }

    private boolean isLeader(Clan clan, UUID member) {
        if (clan.owner.equals(member.toString())) return true;
        String r = clan.memberRanks.get(member.toString());
        if (r == null) return false;
        String perms = clan.ranks.get(r);
        return perms != null && (perms.contains("leader") || perms.contains("admin"));
    }

    public synchronized ClanResult war(UUID owner, String targetClan, boolean start) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        Clan target = clans.get(targetClan.toLowerCase());
        if (target == null) return ClanResult.UNKNOWN_CLAN;
        if (target == clan) return ClanResult.SELF;
        if (start) {
            if (clan.wars.contains(target.name)) return ClanResult.ALREADY_WAR;
            clan.wars.add(target.name);
            target.wars.add(clan.name);
            persist();
            return ClanResult.WAR_STARTED;
        } else {
            if (!clan.wars.contains(target.name)) return ClanResult.NO_WAR;
            clan.wars.remove(target.name);
            target.wars.remove(clan.name);
            clan.warsWon++;
            persist();
            return ClanResult.WAR_ENDED;
        }
    }

    public synchronized ClanResult setRank(UUID owner, UUID target, String rankName) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        if (!clan.members.contains(target.toString())) return ClanResult.NOT_MEMBER;
        if (rankName == null || rankName.isEmpty()) {
            clan.memberRanks.remove(target.toString());
        } else {
            if (!clan.ranks.containsKey(rankName)) return ClanResult.UNKNOWN_CLAN; // Using UNKNOWN_CLAN loosely here, logic usually returns custom result or generic
            clan.memberRanks.put(target.toString(), rankName);
        }
        persist();
        return ClanResult.RANK_SET;
    }

    public synchronized ClanResult createRank(UUID owner, String name) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        clan.ranks.put(name, "");
        persist();
        return ClanResult.RANK_CREATED;
    }

    public synchronized ClanResult deleteRank(UUID owner, String name) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        clan.ranks.remove(name);
        clan.memberRanks.entrySet().removeIf(e -> e.getValue().equals(name));
        persist();
        return ClanResult.RANK_DELETED;
    }

    public synchronized List<String> listRanks(UUID member) {
        Clan clan = clanOf(member);
        if (clan == null) return null;
        return new ArrayList<>(clan.ranks.keySet());
    }

    public synchronized ClanResult setHome(UUID owner, Location loc) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        clan.home = loc;
        persist();
        return ClanResult.HOME_SET;
    }

    public synchronized Location getHome(UUID member) {
        Clan clan = clanOf(member);
        return clan != null ? clan.home : null;
    }

    public synchronized ClanResult setDescription(UUID owner, String desc) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        clan.description = desc;
        persist();
        return ClanResult.DESC_SET;
    }

    public synchronized ClanResult verify(String clanName) {
        Clan clan = clans.get(clanName.toLowerCase());
        if (clan == null) return ClanResult.UNKNOWN_CLAN;
        clan.verified = true;
        persist();
        return ClanResult.VERIFIED;
    }

    public synchronized ClanResult banPlayer(UUID target) {
        banned.add(target.toString());
        Clan c = clanOf(target);
        if (c != null) {
            c.members.remove(target.toString());
        }
        persist();
        return ClanResult.BANNED;
    }

    public synchronized ClanResult unbanPlayer(UUID target) {
        banned.remove(target.toString());
        persist();
        return ClanResult.UNBANNED;
    }

    public synchronized ClanResult setFriendlyFire(UUID owner, boolean on) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        clan.friendlyFire = on;
        persist();
        return ClanResult.FF_TOGGLED;
    }

    public synchronized void setPersonalFf(UUID member, boolean on) {
        if (on) personalFfOff.remove(member.toString());
        else personalFfOff.add(member.toString());
        persist();
    }

    public synchronized ClanResult postBulletin(UUID member, String msg) {
        Clan clan = clanOf(member);
        if (clan == null) return ClanResult.NO_CLAN;
        if (clan.bulletinBoard.size() >= 10) clan.bulletinBoard.remove(0);
        clan.bulletinBoard.add(msg);
        persist();
        return ClanResult.BB_POSTED;
    }

    public synchronized List<String> getBulletins(UUID member) {
        Clan clan = clanOf(member);
        return clan == null ? Collections.emptyList() : new ArrayList<>(clan.bulletinBoard);
    }

    public synchronized String getStats(String clanName) {
        Clan clan = clans.get(clanName.toLowerCase());
        if (clan == null) return null;
        int k = clan.kills.values().stream().mapToInt(Integer::intValue).sum();
        int d = clan.deaths.values().stream().mapToInt(Integer::intValue).sum();
        return k + " Kills / " + d + " Deaths / " + clan.warsWon + " Wars Won";
    }

    public synchronized String getRoster(String clanName) {
        Clan clan = clans.get(clanName.toLowerCase());
        if (clan == null) return null;
        List<String> names = new ArrayList<>();
        org.bukkit.Server server = null;
        try {
            server = Bukkit.getServer();
        } catch (Exception e) {
            server = null;
        }
        for (String id : clan.members) {
            String name = id;
            try {
                if (server != null) {
                    org.bukkit.entity.Player online = server.getPlayer(UUID.fromString(id));
                    name = online != null ? online.getName() : id.substring(0, 8);
                }
            } catch (IllegalArgumentException e) {
                name = id;
            }
            names.add(name);
        }
        return String.join(", ", names);
    }

    public synchronized List<Clan> listClans() {
        return new ArrayList<>(clans.values());
    }

    public synchronized String lookupPlayer(UUID target) {
        Clan clan = clanOf(target);
        if (clan == null) return null;
        String r = clan.memberRanks.getOrDefault(target.toString(), "Member");
        if (clan.owner.equals(target.toString())) r = "Owner";
        return clan.name + " (" + r + ")";
    }

    public synchronized ClanResult trust(UUID owner, UUID target) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        clan.trusted.add(target.toString());
        persist();
        return ClanResult.TRUSTED;
    }

    public synchronized ClanResult untrust(UUID owner, UUID target) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        clan.trusted.remove(target.toString());
        persist();
        return ClanResult.UNTRUSTED;
    }

    public synchronized ClanResult muteMember(UUID owner, UUID target) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        if (!clan.members.contains(target.toString())) return ClanResult.NOT_MEMBER;
        clan.muted.add(target.toString());
        persist();
        return ClanResult.MEMBER_MUTED;
    }

    public synchronized ClanResult setBanner(UUID owner, String serialized) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, owner)) return ClanResult.NOT_LEADER;
        clan.banner = serialized;
        persist();
        return ClanResult.BANNER_SET;
    }

    public synchronized void recordKill(UUID killer, UUID victim) {
        Clan kClan = clanOf(killer);
        if (kClan != null) {
            kClan.kills.put(killer.toString(), kClan.kills.getOrDefault(killer.toString(), 0) + 1);
        }
        Clan vClan = clanOf(victim);
        if (vClan != null) {
            vClan.deaths.put(victim.toString(), vClan.deaths.getOrDefault(victim.toString(), 0) + 1);
        }
        if (kClan != null || vClan != null) persist();
    }

    public synchronized Map<String, Integer> getKills(UUID member) {
        Clan clan = clanOf(member);
        return clan != null ? new HashMap<>(clan.kills) : Collections.emptyMap();
    }

    public synchronized String getMostKilled(UUID member) {
        Clan clan = clanOf(member);
        if (clan == null || clan.kills.isEmpty()) return "None";
        return clan.kills.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("None");
    }

    public synchronized ClanResult rename(UUID owner, String newName) {
        Clan clan = clanOf(owner);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!clan.owner.equals(owner.toString())) return ClanResult.NOT_OWNER;
        if (newName == null || !NAME_PATTERN.matcher(newName).matches()) return ClanResult.BAD_NAME;
        if (clans.containsKey(newName.toLowerCase())) return ClanResult.EXISTS;
        clans.remove(clan.name.toLowerCase());
        clan.name = newName;
        clans.put(newName.toLowerCase(), clan);
        persist();
        return ClanResult.RENAMED;
    }

    public synchronized ClanResult setLocale(UUID member, String locale) {
        Clan clan = clanOf(member);
        if (clan == null) return ClanResult.NO_CLAN;
        if (!isLeader(clan, member)) return ClanResult.NOT_LEADER;
        clan.locale = locale;
        persist();
        return ClanResult.LOCALE_SET;
    }

    public synchronized void setGlobalFf(boolean on) {
        globalFf = on;
        persist();
    }

    public synchronized boolean globalFf() {
        return globalFf;
    }

    public synchronized boolean areFriendly(UUID a, UUID b) {
        if (globalFf) {
            return false;
        }
        Clan clanA = clanOf(a);
        Clan clanB = clanOf(b);
        if (clanA != null && clanB != null && clanA == clanB) {
            if (!clanA.friendlyFire) return true;
        }
        if (clanA != null && clanB != null && clanA.allies.contains(clanB.name)) {
            return true;
        }
        if (personalFfOff.contains(a.toString()) || personalFfOff.contains(b.toString())) return true;
        return false;
    }
}
