/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.boot;

import java.util.List;

public record BootLine(List<Segment> segments) {
    public record Segment(String text, int color) {
    }

    public BootLine(String text, int color) {
        this(List.of(new Segment(text, color)));
    }

    public BootLine {
        segments = List.copyOf(segments);
    }
}
