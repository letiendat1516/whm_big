package org.example.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password hashing sử dụng SHA-256 + salt (Java thuần, không cần thư viện).
 *
 * Format lưu DB: "salt:hash"
 * Ví dụ: "abc123:e3b0c44298fc..."
 */
public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Hash mật khẩu với salt ngẫu nhiên */
    public static String hash(String password) {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        String hashed = sha256(salt + password);
        return salt + ":" + hashed;
    }

    /** Verify mật khẩu so với hash đã lưu */
    public static boolean verify(String password, String storedHash) {
        if (storedHash == null || !storedHash.contains(":")) return false;
        String[] parts = storedHash.split(":", 2);
        String salt = parts[0];
        String expectedHash = parts[1];
        String actualHash = sha256(salt + password);
        return expectedHash.equals(actualHash);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

