/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.boot;

import dev.zellys.nexus.common.boot.BootLine;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

public final class PaperBoot {
    private PaperBoot() {
    }

    public static void send(Audience audience, List<BootLine> lines) {
        for (BootLine line : lines) {
            TextComponent.Builder row = Component.text();
            for (BootLine.Segment segment : line.segments()) {
                row.append(Component.text(segment.text(), TextColor.color(segment.color())));
            }
            audience.sendMessage(row.build());
        }
    }
}
