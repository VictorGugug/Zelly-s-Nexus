/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.zellys.nexus.common.translation.TranslationKey;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZDialogTest {

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> new ZDialog("  ", TranslationKey.BOOT_VERSION, List.of()));
    }

    @Test
    void rejectsNullTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new ZDialog("about", null, List.of()));
    }

    @Test
    void nullBodyBecomesEmpty() {
        assertTrue(new ZDialog("about", TranslationKey.BOOT_VERSION, null).body().isEmpty());
    }

    @Test
    void bodyListIsCopied() {
        List<TranslationKey> source = new ArrayList<>(List.of(TranslationKey.BOOT_PLATFORM));
        ZDialog dialog = new ZDialog("about", TranslationKey.BOOT_VERSION, source);
        source.clear();
        assertEquals(List.of(TranslationKey.BOOT_PLATFORM), dialog.body());
    }
}
