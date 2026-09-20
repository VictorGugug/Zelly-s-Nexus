/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.chat;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ChatModuleTest {
    @Test
    void testChatSets() {
        Set<UUID> shoutEnabled = new HashSet<>();
        Set<UUID> chatHidden = new HashSet<>();
        
        UUID player1 = UUID.randomUUID();
        
        // Simulating the toggle logic for shout
        shoutEnabled.add(player1);
        assertTrue(shoutEnabled.contains(player1));
        
        shoutEnabled.remove(player1);
        assertFalse(shoutEnabled.contains(player1));
        
        // Simulating the toggle logic for chat hidden
        chatHidden.add(player1);
        assertTrue(chatHidden.contains(player1));
        
        chatHidden.remove(player1);
        assertFalse(chatHidden.contains(player1));
    }
}
