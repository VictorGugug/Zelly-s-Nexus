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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.zellys.nexus.common.dialog.DialogPalette;
import dev.zellys.nexus.common.translation.Translator;
import java.util.List;
import org.junit.jupiter.api.Test;

class BootPrinterTest {

    @Test
    void renderContainsLogoSectionsAndStatuses() {
        Translator translator = new Translator("en");
        BootReport report = new BootReport("0.1.0-alpha",
                List.of(new BootReport.Integration("LuckPerms", false)),
                List.of(new BootReport.Module("translation", true, 3),
                        new BootReport.Module("auth", false, 0)));
        List<BootLine> lines = BootPrinter.render(report, translator);
        String all = lines.stream()
                .flatMap(line -> line.segments().stream())
                .map(BootLine.Segment::text)
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(all.contains("███"));
        assertTrue(all.contains("Modular Server Ecosystem"));
        assertTrue(all.contains("LuckPerms"));
        assertTrue(all.contains("not found"));
        assertTrue(all.contains("loaded in 3ms"));
        assertTrue(all.contains("disabled"));
        assertEquals(DialogPalette.PASTEL_BLUE, lines.get(1).segments().get(0).color());
        assertEquals(DialogPalette.BULLET_YELLOW, lines.get(15).segments().get(0).color());
        assertEquals(DialogPalette.TEXT_GRAY, lines.get(15).segments().get(2).color());
    }
}
