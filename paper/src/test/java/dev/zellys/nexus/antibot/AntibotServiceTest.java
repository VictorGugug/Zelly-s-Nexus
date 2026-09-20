/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.antibot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.zellys.nexus.antibot.AntibotService.Verdict;
import dev.zellys.nexus.common.store.YamlStore;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class AntibotServiceTest {

    private AntibotService service;

    @TempDir
    private Path workDir;

    @BeforeEach
    void setup() {
        service = new AntibotService(new YamlStore(workDir.resolve("antibot.yml")));
    }

    @Test
    void testThrottle() {
        String ip = "10.0.0.1";
        // Max joins is 4, so 5th should be throttled
        assertEquals(Verdict.ALLOW, service.check("Player1", ip));
        assertEquals(Verdict.ALLOW, service.check("Player2", ip));
        assertEquals(Verdict.ALLOW, service.check("Player3", ip));
        assertEquals(Verdict.ALLOW, service.check("Player4", ip));
        assertEquals(Verdict.THROTTLED, service.check("Player5", ip));
    }

    @Test
    void testBlacklist() {
        String ip = "10.0.0.2";
        service.block(ip);
        assertTrue(service.blocked().contains(ip));
        assertEquals(1, service.blockedCount());
        
        assertEquals(Verdict.BLACKLISTED, service.check("Zell", ip));
        
        service.unblock(ip);
        assertFalse(service.blocked().contains(ip));
        assertEquals(Verdict.ALLOW, service.check("Zell", ip));
    }

    @Test
    void testNameValidation() {
        String ip = "10.0.0.3";
        assertEquals(Verdict.ALLOW, service.check("Zelly", ip));
        assertEquals(Verdict.BAD_NAME, service.check("Z", ip)); // too short
        assertEquals(Verdict.BAD_NAME, service.check("Zelly_!*", ip)); // invalid characters
        assertEquals(Verdict.BAD_NAME, service.check("ThisNameIsWayTooLong", ip)); // too long
    }

    @Test
    void testPanicMode() {
        String ip = "10.0.0.4";
        service.setPanic(true);
        assertTrue(service.isPanic());
        assertEquals(Verdict.PANIC, service.check("Zelly", ip));
        
        service.setPanic(false);
        assertFalse(service.isPanic());
        assertEquals(Verdict.ALLOW, service.check("Zelly", ip));
    }
}
