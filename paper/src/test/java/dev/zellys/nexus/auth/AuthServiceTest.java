/*
 * Copyright (c) 2026 Zar
 *
 * This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
 *
 * Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
 * plus this project's additional terms. Both are included in full in the
 * LICENSE file at the root of this repository.
 */
package dev.zellys.nexus.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.zellys.nexus.auth.AuthService.AuthResult;
import dev.zellys.nexus.common.store.YamlStore;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class AuthServiceTest {
    private static final String IP = "10.0.0.1";

    private AuthService service;

    @TempDir
    private Path workDir;

    @BeforeEach
    void setup() {
        service = new AuthService(new YamlStore(workDir.resolve("auth.yml")));
    }

    @Test
    void registerAuthenticatesSession() {
        UUID id = UUID.randomUUID();
        assertEquals(AuthResult.OK, service.register(id, IP, "secret"));
        assertTrue(service.hasAccount(id));
        assertTrue(service.isAuthed(id, IP));
        assertFalse(service.isAuthed(id, "10.0.0.9"));
    }

    @Test
    void registerRejectsDuplicatesShortPasswordsAndIpCap() {
        UUID id = UUID.randomUUID();
        assertEquals(AuthResult.OK, service.register(id, IP, "secret"));
        assertEquals(AuthResult.ALREADY_REGISTERED, service.register(id, IP, "other"));
        assertEquals(AuthResult.SHORT_PASSWORD, service.register(UUID.randomUUID(), "10.0.0.2", "abc"));
        assertEquals(AuthResult.OK, service.register(UUID.randomUUID(), IP, "secret"));
        assertEquals(AuthResult.OK, service.register(UUID.randomUUID(), IP, "secret"));
        assertEquals(AuthResult.TOO_MANY_ACCOUNTS, service.register(UUID.randomUUID(), IP, "secret"));
    }

    @Test
    void loginLogoutUnregister() {
        UUID id = UUID.randomUUID();
        assertEquals(AuthResult.NOT_REGISTERED, service.login(id, IP, "secret"));
        service.register(id, IP, "secret");
        service.logout(id);
        assertFalse(service.isAuthed(id, IP));
        assertEquals(AuthResult.WRONG_PASSWORD, service.login(id, IP, "wrong"));
        assertEquals(AuthResult.OK, service.login(id, IP, "secret"));
        assertEquals(AuthResult.WRONG_PASSWORD, service.unregister(id, "wrong"));
        assertEquals(AuthResult.OK, service.unregister(id, "secret"));
        assertFalse(service.hasAccount(id));
    }

    @Test
    void changePasswordEmailAndAdminDelete() {
        UUID id = UUID.randomUUID();
        service.register(id, IP, "secret");
        assertEquals(AuthResult.SHORT_PASSWORD, service.changePassword(id, "secret", "ab"));
        assertEquals(AuthResult.WRONG_PASSWORD, service.changePassword(id, "wrong", "newsecret"));
        assertEquals(AuthResult.OK, service.changePassword(id, "secret", "newsecret"));
        service.logout(id);
        assertEquals(AuthResult.OK, service.login(id, IP, "newsecret"));
        service.setEmail(id, "zell@example.test");
        assertEquals("zell@example.test", service.getEmail(id));
        assertEquals(AuthResult.OK, service.deleteAccount(id));
        assertEquals(AuthResult.NOT_REGISTERED, service.deleteAccount(id));
    }

    @Test
    void totpOtpPinRemember() {
        UUID id = UUID.randomUUID();
        service.register(id, IP, "secret");
        assertNotNull(service.setupTotp(id));
        assertFalse(service.confirmTotp(id, "000000"));
        String otp = service.generateOtp(id);
        assertTrue(service.verifyOtp(id, otp));
        assertFalse(service.verifyOtp(id, otp));
        assertFalse(service.verifyOtp(id, "NOPE1234"));
        service.disableTotp(id);
        service.setPin(id, "1234");
        assertTrue(service.verifyPin(id, "1234"));
        assertFalse(service.verifyPin(id, "0000"));
        service.resetPin(id);
        assertFalse(service.verifyPin(id, "1234"));
        service.setRemember(id, true);
        assertTrue(service.isRemembered(id));
        service.logout(id);
        assertTrue(service.isAuthed(id, IP));
    }

    @Test
    void premiumAutoLoginRespectsMode() {
        UUID premium = UUID.randomUUID();
        service.register(premium, IP, "secret");
        service.setPremium(premium);
        assertEquals("premium", service.accountMode(premium));
        service.logout(premium);
        assertTrue(service.autoLoginIfPremium(premium, "Zell", IP));

        UUID cracked = UUID.randomUUID();
        service.register(cracked, "10.0.0.3", "secret");
        service.setCracked(cracked);
        service.logout(cracked);
        assertFalse(service.autoLoginIfPremium(cracked, "Zell", "10.0.0.3"));

        UUID offline = AuthService.offlineUuid("Zell");
        service.register(offline, "10.0.0.4", "secret");
        service.setFreemium(offline);
        assertEquals("auto", service.accountMode(offline));
        service.logout(offline);
        assertFalse(service.autoLoginIfPremium(offline, "Zell", "10.0.0.4"));

        UUID auto = UUID.randomUUID();
        service.register(auto, "10.0.0.5", "secret");
        service.logout(auto);
        assertTrue(service.autoLoginIfPremium(auto, "Zell", "10.0.0.5"));

        assertFalse(service.autoLoginIfPremium(UUID.randomUUID(), "Nobody", IP));
        assertTrue(AuthService.looksPremium(UUID.randomUUID(), "Zell"));
        assertFalse(AuthService.looksPremium(AuthService.offlineUuid("Zell"), "Zell"));
    }

    @Test
    void captchaAndVerification() {
        UUID id = UUID.randomUUID();
        service.register(id, IP, "secret");
        String captcha = service.issueCaptcha(id);
        assertEquals(4, captcha.length());
        assertFalse(service.answerCaptcha(id, "XXXX"));
        assertTrue(service.answerCaptcha(id, captcha.toLowerCase()));
        assertTrue(service.hasPassedCaptcha(id));
        String code = service.issueVerification(id);
        assertEquals(6, code.length());
        assertFalse(service.confirmVerification(id, "000000"));
        assertTrue(service.confirmVerification(id, code));
        assertTrue(service.isVerified(id));
    }

    @Test
    void resetPasswordAndReload() {
        UUID id = UUID.randomUUID();
        assertEquals(AuthResult.NOT_REGISTERED, service.resetPassword(id, "newsecret"));
        service.register(id, IP, "secret");
        assertEquals(AuthResult.SHORT_PASSWORD, service.resetPassword(id, "ab"));
        assertEquals(AuthResult.OK, service.resetPassword(id, "newsecret"));
        service.logout(id);
        assertEquals(AuthResult.OK, service.login(id, IP, "newsecret"));
        AuthService reloaded = new AuthService(new YamlStore(workDir.resolve("auth.yml")));
        assertTrue(reloaded.hasAccount(id));
        service.logout(id);
        service.reload();
        assertFalse(service.isAuthed(id, IP));
        assertEquals(AuthResult.OK, service.login(id, IP, "newsecret"));
    }

    @Test
    void logsForceLoginAndPurge() {
        UUID id = UUID.randomUUID();
        service.register(id, IP, "secret");
        service.addLog(id, IP, "login");
        assertFalse(service.getLogs(id).isEmpty());
        service.logout(id);
        service.forceLogin(id, IP);
        assertTrue(service.isAuthed(id, IP));
        UUID old = UUID.randomUUID();
        service.register(old, "10.0.0.6", "secret");
        assertEquals(2, service.purgeInactive(-1));
        assertNull(service.getEmail(old));
    }
}
