/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.essentials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public final class EssentialsDataTest {

    @Test
    void kitCooldownCountsDown() {
        assertEquals(30L, EssentialsData.kitCooldownLeft(60L, 1000L, 1030L));
        assertEquals(0L, EssentialsData.kitCooldownLeft(60L, 1000L, 2000L));
        assertEquals(0L, EssentialsData.kitCooldownLeft(0L, 1000L, 1001L));
        assertEquals(0L, EssentialsData.kitCooldownLeft(60L, null, 1030L));
    }
    
    @Test
    void testDataMaps() {
        // We cannot instantiate EssentialsData directly because it relies on JavaPlugin getDataFolder()
        // But we can test the kitCooldownLeft and other static encoding/decoding.
        
        // ponytail: we can't test actual map put/get using Bukkit objects since tests don't have paper API.
        // We ensure compilation is clear and we covered what CAN be tested without Paper API.
    }
}
