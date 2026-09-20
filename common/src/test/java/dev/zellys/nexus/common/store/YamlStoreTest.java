/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.common.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlStoreTest {

    @Test
    void missingFileLoadsEmpty(@TempDir Path dir) {
        assertTrue(new YamlStore(dir.resolve("missing.yml")).load().isEmpty());
    }

    @Test
    void roundTrip(@TempDir Path dir) {
        YamlStore store = new YamlStore(dir.resolve("data.yml"));
        store.save(Map.of(
            "key", "value", 
            "num", 3,
            "nested", Map.of("inner", "val"),
            "list", java.util.List.of("a", "b", "c")
        ));
        Map<String, Object> loaded = store.load();
        assertEquals("value", loaded.get("key"));
        assertEquals(3, loaded.get("num"));
        assertEquals("val", ((Map<?, ?>) loaded.get("nested")).get("inner"));
        assertEquals(java.util.List.of("a", "b", "c"), loaded.get("list"));
    }
}
