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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

class TranslatorTest {

    @Test
    void englishContainsEveryKey() {
        Translator translator = new Translator("en");
        for (TranslationKey key : TranslationKey.values()) {
            assertNotEquals(key.key(), translator.get(key), "missing english text for " + key.key());
        }
    }

    @Test
    void spanishIsComplete() {
        assertEquals(100, new Translator("es").completion("es"));
    }

    @Test
    void unknownLanguageFallsBackToEnglish() {
        Translator translator = new Translator("xx");
        assertEquals("en", translator.activeLanguage());
        assertEquals(new Translator("en").get(TranslationKey.BOOT_VERSION), translator.get(TranslationKey.BOOT_VERSION));
    }

    @Test
    void unknownLanguageCompletionIsZero() {
        assertEquals(0, new Translator("en").completion("xx"));
    }
}
