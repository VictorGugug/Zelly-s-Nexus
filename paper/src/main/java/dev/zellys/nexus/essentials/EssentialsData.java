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
import dev.zellys.nexus.common.store.YamlStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EssentialsData {
    public final Map<UUID, Location> backLocations = new ConcurrentHashMap<>();
    public final Map<UUID, Boolean> tpToggles = new ConcurrentHashMap<>();
    public final Map<UUID, Boolean> godMode = new ConcurrentHashMap<>();
    public final Map<UUID, Boolean> flyMode = new ConcurrentHashMap<>();
    public final Map<UUID, Boolean> vanished = new ConcurrentHashMap<>();
    public final Set<UUID> muted = ConcurrentHashMap.newKeySet();
    public final Map<UUID, String> jailed = new ConcurrentHashMap<>();
    public final Map<UUID, String> nicknames = new ConcurrentHashMap<>();
    public final Map<UUID, Boolean> socialSpy = new ConcurrentHashMap<>();
    public final Map<UUID, Boolean> msgToggle = new ConcurrentHashMap<>();
    public final Map<UUID, UUID> lastMessager = new ConcurrentHashMap<>();
    public final Map<UUID, Map<Material, String>> powertools = new ConcurrentHashMap<>();
    public final Set<UUID> powertoolsEnabled = ConcurrentHashMap.newKeySet();
    public final Map<String, Location> jails = new ConcurrentHashMap<>();
    public final Map<String, Kit> kits = new ConcurrentHashMap<>();
    public final Map<UUID, List<String>> mail = new ConcurrentHashMap<>();
    public int tprMin = -1000;
    public int tprMax = 1000;
    public final Set<UUID> unlimited = ConcurrentHashMap.newKeySet();

    public final Map<String, Object> spawnData = new ConcurrentHashMap<>();
    public final Map<String, Object> homesData = new ConcurrentHashMap<>();
    public final Map<String, Object> warpsData = new ConcurrentHashMap<>();

    private final YamlStore spawns;
    private final YamlStore homes;
    private final YamlStore warps;
    private final YamlStore jailsStore;
    private final YamlStore kitsStore;
    private final YamlStore mailStore;
    private final YamlStore jailedStore;
    private final YamlStore lastSeenStore;
    private final YamlStore nicknamesStore;
    
    public final Map<UUID, TpaRequest> tpaRequests = new ConcurrentHashMap<>();
    public final Map<UUID, Long> lastSeen = new ConcurrentHashMap<>();

    public record Kit(String name, long cooldownSeconds, ItemStack[] items, Map<UUID, Long> lastUsed) {}
    public record TpaRequest(UUID sender, UUID target, boolean here, long timestamp) {}

    public static long kitCooldownLeft(long cooldownSeconds, Long lastUsedEpochSeconds, long nowEpochSeconds) {
        if (cooldownSeconds <= 0 || lastUsedEpochSeconds == null) {
            return 0L;
        }
        long left = cooldownSeconds - (nowEpochSeconds - lastUsedEpochSeconds);
        return Math.max(0L, left);
    }

    public EssentialsData(ZellysNexus plugin) {
        this.spawns = new YamlStore(plugin.getDataFolder().toPath().resolve("spawn.yml"));
        this.homes = new YamlStore(plugin.getDataFolder().toPath().resolve("homes.yml"));
        this.warps = new YamlStore(plugin.getDataFolder().toPath().resolve("warps.yml"));
        this.jailsStore = new YamlStore(plugin.getDataFolder().toPath().resolve("jails.yml"));
        this.kitsStore = new YamlStore(plugin.getDataFolder().toPath().resolve("kits.yml"));
        this.mailStore = new YamlStore(plugin.getDataFolder().toPath().resolve("mail.yml"));
        this.jailedStore = new YamlStore(plugin.getDataFolder().toPath().resolve("jailed.yml"));
        this.lastSeenStore = new YamlStore(plugin.getDataFolder().toPath().resolve("lastseen.yml"));
        this.nicknamesStore = new YamlStore(plugin.getDataFolder().toPath().resolve("nicknames.yml"));
        
        load();
    }

    public void load() {
        spawnData.clear();
        for (Map.Entry<String, Object> entry : spawns.load().entrySet()) {
            Location loc = decodeLocation(entry.getValue());
            if (loc != null) spawnData.put(entry.getKey(), loc);
        }
        
        homesData.clear();
        for (Map.Entry<String, Object> entry : homes.load().entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> userHomes) {
                Map<String, Object> decoded = new HashMap<>();
                for (Map.Entry<?, ?> homeEntry : userHomes.entrySet()) {
                    Location loc = decodeLocation(homeEntry.getValue());
                    if (loc != null) decoded.put(homeEntry.getKey().toString(), loc);
                }
                homesData.put(entry.getKey(), decoded);
            }
        }
        
        warpsData.clear();
        for (Map.Entry<String, Object> entry : warps.load().entrySet()) {
            Location loc = decodeLocation(entry.getValue());
            if (loc != null) warpsData.put(entry.getKey(), loc);
        }
        
        Map<String, Object> jMap = jailsStore.load();
        loadKits(kitsStore.load());
        loadMail(mailStore.load());
        loadJailed(jailedStore.load());
        
        lastSeen.clear();
        for (Map.Entry<String, Object> entry : lastSeenStore.load().entrySet()) {
            if (entry.getValue() instanceof Number n) {
                try { lastSeen.put(UUID.fromString(entry.getKey()), n.longValue()); } catch (Exception e) {}
            }
        }
        nicknames.clear();
        for (Map.Entry<String, Object> entry : nicknamesStore.load().entrySet()) {
            if (entry.getValue() instanceof String s) {
                try { nicknames.put(UUID.fromString(entry.getKey()), s); } catch (Exception e) {}
            }
        }
        
        if (jMap != null) {
            for (Map.Entry<String, Object> entry : jMap.entrySet()) {
                Location loc = decodeLocation(entry.getValue());
                if (loc != null) jails.put(entry.getKey(), loc);
            }
        }
    }

    public void saveJails() {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Location> entry : jails.entrySet()) {
            out.put(entry.getKey(), encodeLocation(entry.getValue()));
        }
        jailsStore.save(out);
    }

    @SuppressWarnings("unchecked")
    private void loadKits(Map<String, Object> root) {
        kits.clear();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> node = (Map<String, Object>) entry.getValue();
            long cooldown = 0L;
            if (node.get("cooldown") instanceof Number number) {
                cooldown = number.longValue();
            }
            List<ItemStack> items = new ArrayList<>();
            if (node.get("items") instanceof Iterable<?> raw) {
                for (Object o : raw) {
                    ItemStack stack = decodeItem(o);
                    if (stack != null) {
                        items.add(stack);
                    }
                }
            }
            kits.put(entry.getKey().toLowerCase(), new Kit(entry.getKey(), cooldown, items.toArray(new ItemStack[0]), new HashMap<>()));
        }
    }

    public void saveKits() {
        Map<String, Object> out = new HashMap<>();
        for (Kit kit : kits.values()) {
            Map<String, Object> node = new HashMap<>();
            node.put("cooldown", kit.cooldownSeconds());
            List<Map<String, Object>> items = new ArrayList<>();
            for (ItemStack stack : kit.items()) {
                if (stack != null && !stack.getType().isAir()) {
                    items.add(encodeItem(stack));
                }
            }
            node.put("items", items);
            out.put(kit.name(), node);
        }
        kitsStore.save(out);
    }

    @SuppressWarnings("unchecked")
    private void loadMail(Map<String, Object> root) {
        mail.clear();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            try {
                UUID id = UUID.fromString(entry.getKey());
                List<String> inbox = new ArrayList<>();
                if (entry.getValue() instanceof Iterable<?> raw) {
                    for (Object o : raw) {
                        if (o instanceof String line) {
                            inbox.add(line);
                        }
                    }
                }
                if (!inbox.isEmpty()) {
                    mail.put(id, inbox);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveMail() {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<UUID, List<String>> entry : mail.entrySet()) {
            out.put(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }
        mailStore.save(out);
    }

    private void loadJailed(Map<String, Object> root) {
        jailed.clear();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            try {
                UUID id = UUID.fromString(entry.getKey());
                if (entry.getValue() instanceof String jail) {
                    jailed.put(id, jail);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveJailed() {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<UUID, String> entry : jailed.entrySet()) {
            out.put(entry.getKey().toString(), entry.getValue());
        }
        jailedStore.save(out);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> encodeItem(ItemStack stack) {
        return stack.clone().serialize();
    }

    @SuppressWarnings("unchecked")
    private static ItemStack decodeItem(Object raw) {
        try {
            if (raw instanceof Map) {
                return ItemStack.deserialize((Map<String, Object>) raw);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Map<String, Object> encodeLocation(Location loc) {
        if (loc == null) return null;
        Map<String, Object> lmap = new HashMap<>();
        lmap.put("world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        lmap.put("x", loc.getX());
        lmap.put("y", loc.getY());
        lmap.put("z", loc.getZ());
        lmap.put("yaw", loc.getYaw());
        lmap.put("pitch", loc.getPitch());
        return lmap;
    }

    public static Location decodeLocation(Object obj) {
        if (obj instanceof Map<?, ?> lmap) {
            try {
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(lmap.get("world").toString());
                double x = ((Number) lmap.get("x")).doubleValue();
                double y = ((Number) lmap.get("y")).doubleValue();
                double z = ((Number) lmap.get("z")).doubleValue();
                float yaw = ((Number) lmap.get("yaw")).floatValue();
                float pitch = ((Number) lmap.get("pitch")).floatValue();
                return new Location(w, x, y, z, yaw, pitch);
            } catch (Exception ignored) {}
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void saveHomes() {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : homesData.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> userHomes) {
                Map<String, Object> encoded = new HashMap<>();
                for (Map.Entry<?, ?> homeEntry : userHomes.entrySet()) {
                    if (homeEntry.getValue() instanceof Location loc) {
                        encoded.put(homeEntry.getKey().toString(), encodeLocation(loc));
                    }
                }
                out.put(entry.getKey(), encoded);
            }
        }
        homes.save(out);
    }
    
    public void saveWarps() {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : warpsData.entrySet()) {
            if (entry.getValue() instanceof Location loc) {
                out.put(entry.getKey(), encodeLocation(loc));
            }
        }
        warps.save(out);
    }
    
    public void saveSpawns() {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : spawnData.entrySet()) {
            if (entry.getValue() instanceof Location loc) {
                out.put(entry.getKey(), encodeLocation(loc));
            }
        }
        spawns.save(out);
    }
    
    public void saveLastSeen() {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : lastSeen.entrySet()) {
            out.put(entry.getKey().toString(), entry.getValue());
        }
        lastSeenStore.save(out);
    }

    public void saveNicknames() {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<UUID, String> entry : nicknames.entrySet()) {
            out.put(entry.getKey().toString(), entry.getValue());
        }
        nicknamesStore.save(out);
    }
}
