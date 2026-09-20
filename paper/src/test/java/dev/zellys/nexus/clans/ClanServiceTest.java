/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.clans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.zellys.nexus.clans.ClanService.ClanResult;
import dev.zellys.nexus.common.store.YamlStore;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class ClanServiceTest {
    private ClanService service;

    @BeforeEach
    void setup(@TempDir Path dir) {
        service = new ClanService(new YamlStore(dir.resolve("clans.yml")));
    }

    @Test
    void createInviteAcceptKickLeaveDisband() {
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        assertEquals(ClanResult.BAD_NAME, service.create(owner, "ab"));
        assertEquals(ClanResult.OK, service.create(owner, "Snowflakes"));
        assertEquals(ClanResult.EXISTS, service.create(UUID.randomUUID(), "Snowflakes"));
        assertEquals(ClanResult.ALREADY, service.create(owner, "Other"));
        assertEquals(ClanResult.SELF, service.invite(owner, owner));
        assertEquals(ClanResult.OK, service.invite(owner, member));
        assertEquals(ClanResult.OK, service.accept(member));
        assertNotNull(service.clanOf(member));
        assertEquals(ClanResult.OK, service.kick(owner, member));
        assertNull(service.clanOf(member));
        assertEquals(ClanResult.OWNER_LEAVE, service.leave(owner));
        assertEquals(ClanResult.OK, service.disband(owner));
        assertNull(service.clanOf(owner));
    }

    @Test
    void allyRivalWarAndFriendlyFire() {
        UUID blue = UUID.randomUUID();
        UUID red = UUID.randomUUID();
        service.create(blue, "Blue");
        service.create(red, "Red");
        assertEquals(ClanResult.OK, service.ally(blue, "Red"));
        assertTrue(service.areFriendly(blue, red));
        assertEquals(ClanResult.OK, service.rival(blue, "Red"));
        assertFalse(service.areFriendly(blue, red));
        assertEquals(ClanResult.WAR_STARTED, service.war(blue, "Red", true));
        assertEquals(ClanResult.ALREADY_WAR, service.war(blue, "Red", true));
        assertEquals(ClanResult.WAR_ENDED, service.war(blue, "Red", false));
        assertEquals(ClanResult.OK, service.unally(blue, "Red"));
        assertEquals(ClanResult.OK, service.unrival(blue, "Red"));
        service.setGlobalFf(true);
        assertTrue(service.globalFf());
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        service.create(a, "Alpha");
        service.invite(a, b);
        service.accept(b);
        assertFalse(service.areFriendly(a, b));
        service.setGlobalFf(false);
        assertTrue(service.areFriendly(a, b));
    }

    @Test
    void ranksTrustMuteAndProfile() {
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        service.create(owner, "North");
        service.invite(owner, member);
        service.accept(member);
        assertEquals(ClanResult.RANK_CREATED, service.createRank(owner, "Elder"));
        assertEquals(ClanResult.RANK_SET, service.setRank(owner, member, "Elder"));
        assertTrue(service.listRanks(member).contains("Elder"));
        assertEquals(ClanResult.TRUSTED, service.trust(owner, member));
        assertEquals(ClanResult.MEMBER_MUTED, service.muteMember(owner, member));
        assertEquals(ClanResult.UNTRUSTED, service.untrust(owner, member));
        assertEquals(ClanResult.DESC_SET, service.setDescription(owner, "Cold and proud"));
        assertEquals(ClanResult.VERIFIED, service.verify("North"));
        assertEquals(ClanResult.BANNER_SET, service.setBanner(owner, "white"));
        assertEquals(ClanResult.LOCALE_SET, service.setLocale(owner, "es"));
        assertEquals(ClanResult.RENAMED, service.rename(owner, "South"));
        assertNotNull(service.getRoster("South"));
        assertNotNull(service.lookupPlayer(member));
        service.recordKill(member, UUID.randomUUID());
        assertFalse(service.getStats("South").isEmpty());
        assertFalse(service.getKills(member).isEmpty());
        assertFalse(service.getMostKilled(member).isEmpty());
        assertEquals(ClanResult.BB_POSTED, service.postBulletin(member, "Hello"));
        assertFalse(service.getBulletins(member).isEmpty());
        assertFalse(service.listClans().isEmpty());
        assertEquals(ClanResult.BANNED, service.banPlayer(member));
        assertEquals(ClanResult.UNBANNED, service.unbanPlayer(member));
        assertEquals(ClanResult.RANK_DELETED, service.deleteRank(owner, "Elder"));
    }
}
