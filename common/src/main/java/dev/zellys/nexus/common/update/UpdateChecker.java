/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.update;

import java.util.ArrayList;
import java.util.List;

public final class UpdateChecker {
    public enum Status {
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        CHECK_FAILED
    }

    public record UpdateResult(Status status, String latestVersion, String url) {
    }

    public interface TagSource {
        List<String> tags() throws Exception;
    }

    private UpdateChecker() {
    }

    public static UpdateResult check(String currentVersion, String repository, TagSource source) {
        List<String> tags;
        try {
            tags = new ArrayList<>(source.tags());
        } catch (Exception e) {
            return new UpdateResult(Status.CHECK_FAILED, currentVersion, "");
        }
        Version current = Version.parse(currentVersion);
        Version bestStable = null;
        Version bestAny = null;
        String bestStableTag = currentVersion;
        String bestAnyTag = currentVersion;
        for (String tag : tags) {
            Version candidate;
            try {
                candidate = Version.parse(tag);
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (candidate.compareTo(current) <= 0) {
                continue;
            }
            if (candidate.suffix().isEmpty()) {
                if (bestStable == null || candidate.compareTo(bestStable) > 0) {
                    bestStable = candidate;
                    bestStableTag = tag;
                }
            }
            if (bestAny == null || candidate.compareTo(bestAny) > 0) {
                bestAny = candidate;
                bestAnyTag = tag;
            }
        }
        if (bestStable != null) {
            return new UpdateResult(Status.UPDATE_AVAILABLE, bestStableTag,
                    "https://github.com/" + repository + "/releases/tag/" + bestStableTag);
        }
        if (!current.suffix().isEmpty() && bestAny != null) {
            return new UpdateResult(Status.UPDATE_AVAILABLE, bestAnyTag,
                    "https://github.com/" + repository + "/releases/tag/" + bestAnyTag);
        }
        return new UpdateResult(Status.UP_TO_DATE, currentVersion, "");
    }

    static final class Version implements Comparable<Version> {
        private final int[] parts;
        private final String suffix;

        private Version(int[] parts, String suffix) {
            this.parts = parts;
            this.suffix = suffix;
        }

        static Version parse(String text) {
            String clean = (text.startsWith("v") || text.startsWith("V")) ? text.substring(1) : text;
            String base = clean;
            String suffix = "";
            int dash = clean.indexOf('-');
            if (dash >= 0) {
                base = clean.substring(0, dash);
                suffix = clean.substring(dash + 1);
            }
            String[] raw = base.split("\\.");
            int[] parts = new int[raw.length];
            for (int i = 0; i < raw.length; i++) {
                parts[i] = Integer.parseInt(raw[i]);
            }
            return new Version(parts, suffix);
        }

        String suffix() {
            return suffix;
        }

        @Override
        public int compareTo(Version other) {
            int len = Math.max(parts.length, other.parts.length);
            for (int i = 0; i < len; i++) {
                int a = i < parts.length ? parts[i] : 0;
                int b = i < other.parts.length ? other.parts[i] : 0;
                if (a != b) {
                    return Integer.compare(a, b);
                }
            }
            if (suffix.isEmpty() && !other.suffix.isEmpty()) {
                return 1;
            }
            if (!suffix.isEmpty() && other.suffix.isEmpty()) {
                return -1;
            }
            return suffix.compareTo(other.suffix);
        }
    }
}
