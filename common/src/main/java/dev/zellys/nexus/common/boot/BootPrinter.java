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

import dev.zellys.nexus.common.translation.TranslationKey;
import dev.zellys.nexus.common.translation.Translator;
import java.util.ArrayList;
import java.util.List;

public final class BootPrinter {
    private static final int WIDTH = 41;

    private static final String[] LOGO = {
        "█████ █████ █     █     █   █   █   ███ ",
        "    █ █     █     █     █   █   █  █    ",
        "   █  ████  █     █     █   █   █   ███ ",
        "  █   █     █     █     █   █   █      █",
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

    public static List<String> render(BootReport report, Translator translator) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        for (String row : LOGO) {
            lines.add("§b" + row);
        }
        for (String row : LOGO_LINE_2) {
            lines.add("§b" + row);
        }
        lines.add("§f" + center(translator.get(TranslationKey.BOOT_TAGLINE_1)));
        lines.add("§f" + center(translator.get(TranslationKey.BOOT_TAGLINE_2)));
        lines.add("");
        lines.add("§f" + translator.get(TranslationKey.BOOT_DETECTING));
        for (BootReport.Integration integration : report.integrations()) {
            String status = integration.found()
                    ? "§a" + translator.get(TranslationKey.STATUS_FOUND)
                    : "§7" + translator.get(TranslationKey.STATUS_NOT_FOUND);
            lines.add("§e• §f" + dots(integration.name()) + " " + status);
        }
        lines.add("");
        lines.add("§f" + translator.get(TranslationKey.BOOT_MANAGER));
        for (BootReport.Module module : report.modules()) {
            String status = module.enabled()
                    ? "§a" + translator.get(TranslationKey.STATUS_LOADED_IN, module.millis())
                    : "§7" + translator.get(TranslationKey.STATUS_DISABLED);
            lines.add("§e• §f" + dots(module.name()) + " " + status);
        }
        return lines;
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
