/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.antibot;

import dev.zellys.nexus.common.store.YamlStore;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class AntibotService {
    static final long WINDOW_SECONDS = 10;
    static final int MAX_JOINS_PER_WINDOW = 4;
    static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    public enum Verdict {
        ALLOW,
        BAD_NAME,
        THROTTLED,
        BLACKLISTED,
        PANIC
    }

    private final YamlStore store;
    private final Map<String, Deque<Long>> joins = new HashMap<>();
    private final Deque<Long> globalJoins = new ArrayDeque<>();
    private final Set<String> blacklist = new HashSet<>();
    private volatile boolean panicMode = false;

    @SuppressWarnings("unchecked")
    public AntibotService(YamlStore store) {
        this.store = store;
        Object root = store.load().get("ips");
        if (root instanceof List) {
            for (Object entry : (List<?>) root) {
                if (entry instanceof String) {
                    blacklist.add((String) entry);
                }
            }
        }
    }

    public synchronized Verdict check(String name, String ip) {
        long nowMs = System.currentTimeMillis();
        while (!globalJoins.isEmpty() && globalJoins.peekFirst() < nowMs - 1000) {
            globalJoins.pollFirst();
        }
        globalJoins.addLast(nowMs);

        if (panicMode) {
            return Verdict.PANIC;
        }

        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            return Verdict.BAD_NAME;
        }
        if (blacklist.contains(ip)) {
            return Verdict.BLACKLISTED;
        }
        long now = Instant.now().getEpochSecond();
        Deque<Long> times = joins.computeIfAbsent(ip, key -> new ArrayDeque<>());
        while (!times.isEmpty() && times.peekFirst() < now - WINDOW_SECONDS) {
            times.pollFirst();
        }
        times.addLast(now);
        if (times.size() > MAX_JOINS_PER_WINDOW) {
            return Verdict.THROTTLED;
        }
        return Verdict.ALLOW;
    }

    public synchronized boolean block(String ip) {
        boolean added = blacklist.add(ip);
        if (added) {
            persist();
        }
        return added;
    }

    public synchronized boolean unblock(String ip) {
        boolean removed = blacklist.remove(ip);
        if (removed) {
            persist();
        }
        return removed;
    }

    public synchronized Set<String> blocked() {
        return Set.copyOf(blacklist);
    }

    public synchronized int blockedCount() {
        return blacklist.size();
    }

    public synchronized int connectionRate() {
        long nowMs = System.currentTimeMillis();
        while (!globalJoins.isEmpty() && globalJoins.peekFirst() < nowMs - 1000) {
            globalJoins.pollFirst();
        }
        return globalJoins.size();
    }

    public boolean isPanic() {
        return panicMode;
    }

    public void setPanic(boolean panicMode) {
        this.panicMode = panicMode;
    }

    private void persist() {
        store.save(Map.of("ips", List.copyOf(blacklist)));
    }
}
