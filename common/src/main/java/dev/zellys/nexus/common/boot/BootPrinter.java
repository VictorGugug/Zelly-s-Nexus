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

import dev.zellys.nexus.common.dialog.DialogPalette;
import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import java.util.ArrayList;
import java.util.List;

public final class BootPrinter {
    private static final int WIDTH = 41;

    private static final String[] LOGO = {
        "█████ █████ █     █     █   █   █   ███ ",
        "    █ █     █     █     █   █   █  █    ",
        "   █  ████  █     █      ███    █   ███ ",
        "  █   █     █     █       █     █      █",
        "█████ █████ █████ █████  ███    █   ███ "
    };

    private static final String[] LOGO_LINE_2 = {
        "█   █ █████ █   █ █   █  ███ ",
        "██  █ █     █   █ █   █ █    ",
        "█ █ █ ████   █ █  █   █  ███ ",
        "█  ██ █     █   █ █   █     █",
        "█   █ █████ █   █  ███   ███ "
    };

    private BootPrinter() {
    }

    public static List<BootLine> render(BootReport report, Translator translator) {
        List<BootLine> lines = new ArrayList<>();
        lines.add(new BootLine("", DialogPalette.TEXT_WHITE));
        for (String row : LOGO) {
            lines.add(new BootLine(row, DialogPalette.PASTEL_BLUE));
        }
        for (String row : LOGO_LINE_2) {
            lines.add(new BootLine(row, DialogPalette.PASTEL_BLUE));
        }
        lines.add(new BootLine(center(translator.get(TranslationKey.BOOT_TAGLINE_1)), DialogPalette.TEXT_WHITE));
        lines.add(new BootLine(center(translator.get(TranslationKey.BOOT_TAGLINE_2)), DialogPalette.TEXT_WHITE));
        lines.add(new BootLine("", DialogPalette.TEXT_WHITE));
        lines.add(new BootLine(translator.get(TranslationKey.BOOT_DETECTING), DialogPalette.TEXT_WHITE));
        for (BootReport.Integration integration : report.integrations()) {
            lines.add(statusLine(integration.name(), integration.found()
                    ? translator.get(TranslationKey.STATUS_FOUND)
                    : translator.get(TranslationKey.STATUS_NOT_FOUND),
                    integration.found()));
        }
        lines.add(new BootLine("", DialogPalette.TEXT_WHITE));
        lines.add(new BootLine(translator.get(TranslationKey.BOOT_MANAGER), DialogPalette.TEXT_WHITE));
        for (BootReport.Module module : report.modules()) {
            lines.add(statusLine(module.name(), module.enabled()
                    ? translator.get(TranslationKey.STATUS_LOADED_IN, module.millis())
                    : translator.get(TranslationKey.STATUS_DISABLED),
                    module.enabled()));
        }
        return lines;
    }

    private static BootLine statusLine(String name, String status, boolean good) {
        int color = good ? DialogPalette.PASTEL_MINT : DialogPalette.TEXT_GRAY;
        return new BootLine(List.of(
                new BootLine.Segment("• ", DialogPalette.BULLET_YELLOW),
                new BootLine.Segment(dots(name) + " ", DialogPalette.TEXT_WHITE),
                new BootLine.Segment(status, color)));
    }

    static String center(String text) {
        int pad = Math.max(0, (WIDTH - text.length()) / 2);
        return " ".repeat(pad) + text;
    }

    static String dots(String name) {
        StringBuilder out = new StringBuilder(name);
        while (out.length() < 20) {
            out.append('.');
        }
        return out.toString();
    }
}
