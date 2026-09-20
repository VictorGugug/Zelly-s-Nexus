/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.itemedit;

import dev.zellys.nexus.common.store.YamlStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ItemStorageTest {
    private Path tempDir;
    private YamlStore store;
    
    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("nexus-itemedit");
        // ponytail: testing YamlStore layer directly since ItemStack requires Paper runtime
        store = new YamlStore(tempDir.resolve("items.yml"));
    }
    
    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(tempDir.resolve("items.yml"));
        Files.deleteIfExists(tempDir);
    }
    
    @Test
    void testSaveAndGetAndDeleteAndList() {
        // Test save
        Map<String, Object> data = store.load();
        assertTrue(data.isEmpty());
        
        String fakeItemData = Base64.getEncoder().encodeToString("fake_item".getBytes());
        data.put("test_key", fakeItemData);
        store.save(data);
        
        // Test list and get
        Map<String, Object> loaded = store.load();
        assertEquals(1, loaded.size());
        assertTrue(loaded.containsKey("test_key"));
        assertEquals(fakeItemData, loaded.get("test_key"));
        
        // Test delete
        loaded.remove("test_key");
        store.save(loaded);
        
        Map<String, Object> afterDelete = store.load();
        assertTrue(afterDelete.isEmpty());
    }
}
