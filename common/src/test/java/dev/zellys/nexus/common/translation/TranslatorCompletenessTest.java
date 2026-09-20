/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public final class TranslatorCompletenessTest {
    @Test
    void englishAndSpanishCoverEveryKey() {
        Translator translator = new Translator("en");
        assertEquals(100, translator.completion("en"));
        assertEquals(100, translator.completion("es"));
    }
}
