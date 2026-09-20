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

import at.favre.lib.crypto.bcrypt.BCrypt;
import dev.zellys.nexus.common.store.YamlStore;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AuthService {
    static final long SESSION_SECONDS = 12 * 60 * 60;
    static final int MAX_ACCOUNTS_PER_IP = 3;
    static final int MIN_PASSWORD_LENGTH = 4;
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public enum AuthResult {
        OK,
        ALREADY_REGISTERED,
        NOT_REGISTERED,
        WRONG_PASSWORD,
        TOO_MANY_ACCOUNTS,
        SHORT_PASSWORD
    }

    private final YamlStore store;
    private final Map<String, Map<String, Object>> users = new HashMap<>();
    private final Map<UUID, Long> sessions = new HashMap<>();
    private final Map<UUID, String> lastIp = new HashMap<>();
    private final Map<UUID, String> pendingTotp = new HashMap<>();
    private final Map<UUID, OtpData> otpData = new HashMap<>();
    private final Map<UUID, String> pendingCaptcha = new HashMap<>();
    private final Map<UUID, String> pendingVerification = new HashMap<>();
    private final Set<UUID> captchaPassed = new HashSet<>();
    private final Set<UUID> verified = new HashSet<>();

    private static class OtpData {
        String code;
        long expiry;
        OtpData(String code, long expiry) {
            this.code = code;
            this.expiry = expiry;
        }
    }

    @SuppressWarnings("unchecked")
    public AuthService(YamlStore store) {
        this.store = store;
        loadUsers();
    }

    public void reload() {
        users.clear();
        sessions.clear();
        loadUsers();
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        Object root = store.load().get("users");
        if (root instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) root).entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof Map) {
                    users.put((String) entry.getKey(), new HashMap<>((Map<String, Object>) entry.getValue()));
                }
            }
        }
    }

    public boolean hasAccount(UUID uuid) {
        return users.containsKey(uuid.toString());
    }

    public AuthResult register(UUID uuid, String ip, String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return AuthResult.SHORT_PASSWORD;
        }
        if (hasAccount(uuid)) {
            return AuthResult.ALREADY_REGISTERED;
        }
        if (countAccounts(ip) >= MAX_ACCOUNTS_PER_IP) {
            return AuthResult.TOO_MANY_ACCOUNTS;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("hash", BCrypt.withDefaults().hashToString(10, password.toCharArray()));
        data.put("ip", ip);
        data.put("mode", "auto");
        data.put("seen", Instant.now().getEpochSecond());
        users.put(uuid.toString(), data);
        persist();
        authenticate(uuid, ip);
        return AuthResult.OK;
    }

    public AuthResult login(UUID uuid, String ip, String password) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data == null) {
            return AuthResult.NOT_REGISTERED;
        }
        Object hash = data.get("hash");
        if (!(hash instanceof String) || !BCrypt.verifyer().verify(password.toCharArray(), ((String) hash).toCharArray()).verified) {
            return AuthResult.WRONG_PASSWORD;
        }
        data.put("seen", Instant.now().getEpochSecond());
        persist();
        authenticate(uuid, ip);
        return AuthResult.OK;
    }

    public void logout(UUID uuid) {
        sessions.remove(uuid);
        lastIp.remove(uuid);
    }

    public AuthResult unregister(UUID uuid, String password) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data == null) {
            return AuthResult.NOT_REGISTERED;
        }
        Object hash = data.get("hash");
        if (!(hash instanceof String) || !BCrypt.verifyer().verify(password.toCharArray(), ((String) hash).toCharArray()).verified) {
            return AuthResult.WRONG_PASSWORD;
        }
        users.remove(uuid.toString());
        logout(uuid);
        persist();
        return AuthResult.OK;
    }

    public AuthResult resetPassword(UUID uuid, String newPassword) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data == null) {
            return AuthResult.NOT_REGISTERED;
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return AuthResult.SHORT_PASSWORD;
        }
        data.put("hash", BCrypt.withDefaults().hashToString(10, newPassword.toCharArray()));
        persist();
        return AuthResult.OK;
    }

    public AuthResult deleteAccount(UUID target) {
        if (users.remove(target.toString()) == null) {
            return AuthResult.NOT_REGISTERED;
        }
        logout(target);
        persist();
        return AuthResult.OK;
    }

    public AuthResult changePassword(UUID uuid, String oldPassword, String newPassword) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data == null) {
            return AuthResult.NOT_REGISTERED;
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return AuthResult.SHORT_PASSWORD;
        }
        Object hash = data.get("hash");
        if (!(hash instanceof String) || !BCrypt.verifyer().verify(oldPassword.toCharArray(), ((String) hash).toCharArray()).verified) {
            return AuthResult.WRONG_PASSWORD;
        }
        data.put("hash", BCrypt.withDefaults().hashToString(10, newPassword.toCharArray()));
        persist();
        return AuthResult.OK;
    }

    public void setEmail(UUID uuid, String email) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data != null) {
            data.put("email", email);
            persist();
        }
    }

    public String getEmail(UUID uuid) {
        Map<String, Object> data = users.get(uuid.toString());
        return data != null && data.get("email") instanceof String ? (String) data.get("email") : null;
    }

    public String setupTotp(UUID uuid) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        String secret = encodeBase32(bytes);
        pendingTotp.put(uuid, secret);
        return secret;
    }

    public boolean confirmTotp(UUID uuid, String code) {
        String secret = pendingTotp.get(uuid);
        if (secret != null && verifyTotp(secret, code)) {
            Map<String, Object> data = users.get(uuid.toString());
            if (data != null) {
                data.put("totp", secret);
                pendingTotp.remove(uuid);
                persist();
                return true;
            }
        }
        return false;
    }

    public void disableTotp(UUID uuid) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data != null) {
            data.remove("totp");
            persist();
        }
    }

    public String generateOtp(UUID uuid) {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String code = sb.toString();
        otpData.put(uuid, new OtpData(code, Instant.now().getEpochSecond() + 300));
        return code;
    }

    public boolean verifyOtp(UUID uuid, String code) {
        OtpData data = otpData.get(uuid);
        if (data != null && data.code.equals(code) && Instant.now().getEpochSecond() <= data.expiry) {
            otpData.remove(uuid);
            return true;
        }
        return false;
    }

    public String issueCaptcha(UUID uuid) {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String code = sb.toString();
        pendingCaptcha.put(uuid, code);
        captchaPassed.remove(uuid);
        return code;
    }

    public boolean answerCaptcha(UUID uuid, String code) {
        String pending = pendingCaptcha.get(uuid);
        if (pending != null && pending.equalsIgnoreCase(code)) {
            pendingCaptcha.remove(uuid);
            captchaPassed.add(uuid);
            return true;
        }
        return false;
    }

    public boolean hasPassedCaptcha(UUID uuid) {
        return captchaPassed.contains(uuid);
    }

    public String issueVerification(UUID uuid) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        String code = sb.toString();
        pendingVerification.put(uuid, code);
        verified.remove(uuid);
        return code;
    }

    public boolean confirmVerification(UUID uuid, String code) {
        String pending = pendingVerification.get(uuid);
        if (pending != null && pending.equals(code)) {
            pendingVerification.remove(uuid);
            verified.add(uuid);
            return true;
        }
        return false;
    }

    public boolean isVerified(UUID uuid) {
        return verified.contains(uuid);
    }

    public void setPin(UUID uuid, String pin) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data != null) {
            data.put("pin", BCrypt.withDefaults().hashToString(10, pin.toCharArray()));
            persist();
        }
    }

    public boolean verifyPin(UUID uuid, String pin) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data != null && data.get("pin") instanceof String hash) {
            return BCrypt.verifyer().verify(pin.toCharArray(), hash.toCharArray()).verified;
        }
        return false;
    }

    public void resetPin(UUID uuid) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data != null) {
            data.remove("pin");
            persist();
        }
    }

    public void setRemember(UUID uuid, boolean remember) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data != null) {
            data.put("remember", remember);
            persist();
        }
    }

    public boolean isRemembered(UUID uuid) {
        Map<String, Object> data = users.get(uuid.toString());
        return data != null && Boolean.TRUE.equals(data.get("remember"));
    }

    public void setPremium(UUID uuid) {
        setMode(uuid, "premium");
    }

    public void setCracked(UUID uuid) {
        setMode(uuid, "cracked");
    }

    public void setFreemium(UUID uuid) {
        setMode(uuid, "auto");
    }

    private void setMode(UUID uuid, String mode) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data != null) {
            data.put("mode", mode);
            persist();
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getLogs(UUID uuid) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data != null && data.get("logs") instanceof List) {
            return (List<String>) data.get("logs");
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public void addLog(UUID uuid, String ip, String action) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data != null) {
            List<String> logs;
            if (data.get("logs") instanceof List) {
                logs = new ArrayList<>((List<String>) data.get("logs"));
            } else {
                logs = new ArrayList<>();
            }
            logs.add(Instant.now().toString() + " | " + ip + " | " + action);
            if (logs.size() > 50) logs.remove(0);
            data.put("logs", logs);
            persist();
        }
    }

    public int purgeInactive(long daysOld) {
        long cutoff = Instant.now().getEpochSecond() - (daysOld * 86400);
        int purged = 0;
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : users.entrySet()) {
            Object seen = entry.getValue().get("seen");
            if (seen instanceof Number && ((Number) seen).longValue() < cutoff) {
                toRemove.add(entry.getKey());
            }
        }
        for (String id : toRemove) {
            users.remove(id);
            purged++;
        }
        if (purged > 0) {
            persist();
        }
        return purged;
    }

    public void forceLogin(UUID uuid, String ip) {
        if (hasAccount(uuid)) {
            authenticate(uuid, ip);
        }
    }

    public static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static boolean looksPremium(UUID uuid, String name) {
        return name != null && !uuid.equals(offlineUuid(name));
    }

    public String accountMode(UUID uuid) {
        Map<String, Object> data = users.get(uuid.toString());
        if (data != null && data.get("mode") instanceof String mode) {
            return mode;
        }
        return "auto";
    }

    public boolean autoLoginIfPremium(UUID uuid, String name, String ip) {
        if (!hasAccount(uuid) || isAuthed(uuid, ip)) {
            return isAuthed(uuid, ip);
        }
        String mode = accountMode(uuid);
        if (mode.equals("cracked")) {
            return false;
        }
        if (looksPremium(uuid, name)) {
            Map<String, Object> data = users.get(uuid.toString());
            if (data != null) {
                data.put("seen", Instant.now().getEpochSecond());
                persist();
            }
            authenticate(uuid, ip);
            addLog(uuid, ip, "autologin");
            return true;
        }
        return false;
    }

    public boolean isAuthed(UUID uuid, String ip) {
        if (isRemembered(uuid) && ip.equals(users.get(uuid.toString()).get("ip"))) {
            return true;
        }
        Long expiry = sessions.get(uuid);
        return expiry != null && expiry > Instant.now().getEpochSecond() && ip.equals(lastIp.get(uuid));
    }

    private void authenticate(UUID uuid, String ip) {
        sessions.put(uuid, Instant.now().getEpochSecond() + SESSION_SECONDS);
        lastIp.put(uuid, ip);
    }

    private int countAccounts(String ip) {
        int count = 0;
        for (Map<String, Object> data : users.values()) {
            if (ip.equals(data.get("ip"))) {
                count++;
            }
        }
        return count;
    }

    private void persist() {
        store.save(Map.of("users", new HashMap<>(users)));
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer <<= 8;
            buffer |= (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                result.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            buffer <<= (5 - bitsLeft);
            result.append(BASE32_CHARS.charAt(buffer & 0x1F));
        }
        return result.toString();
    }

    private static byte[] decodeBase32(String base32) {
        base32 = base32.toUpperCase();
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (char c : base32.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val >= 0) {
                bitsLeft += 5;
                if (bitsLeft >= 8) {
                    bitsLeft -= 8;
                    count++;
                }
            }
        }
        byte[] result = new byte[count];
        buffer = 0;
        bitsLeft = 0;
        count = 0;
        for (char c : base32.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val >= 0) {
                buffer <<= 5;
                buffer |= val;
                bitsLeft += 5;
                if (bitsLeft >= 8) {
                    bitsLeft -= 8;
                    result[count++] = (byte) (buffer >> bitsLeft);
                }
            }
        }
        return result;
    }

    private static boolean verifyTotp(String secret, String code) {
        try {
            byte[] key = decodeBase32(secret);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            long timeWindow = Instant.now().getEpochSecond() / 30;
            for (int i = -1; i <= 1; i++) {
                if (generateTotpCode(mac, timeWindow + i).equals(code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String generateTotpCode(Mac mac, long time) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(time);
        byte[] hash = mac.doFinal(buffer.array());
        int offset = hash[hash.length - 1] & 0xF;
        int binary = ((hash[offset] & 0x7F) << 24) |
                ((hash[offset + 1] & 0xFF) << 16) |
                ((hash[offset + 2] & 0xFF) << 8) |
                (hash[offset + 3] & 0xFF);
        int otp = binary % 1000000;
        return String.format("%06d", otp);
    }
}
