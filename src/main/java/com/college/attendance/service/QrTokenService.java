package com.college.attendance.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class QrTokenService {

    private static final long WINDOW_SECONDS = 10;
    private final byte[] secret = new byte[32];

    public QrTokenService() {
        new SecureRandom().nextBytes(secret);
    }

    private long currentWindow() {
        return Instant.now().getEpochSecond() / WINDOW_SECONDS;
    }

    private String sign(long sessionId, long window) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] hash = mac.doFinal((sessionId + ":" + window).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String currentPayload(long sessionId) {
        long window = currentWindow();
        return sessionId + "." + window + "." + sign(sessionId, window);
    }

    public boolean isValid(String payload, long sessionId) {
        try {
            String[] parts = payload.trim().split("\\.");
            if (parts.length != 3) return false;
            if (Long.parseLong(parts[0]) != sessionId) return false;
            long window = Long.parseLong(parts[1]);
            long now = currentWindow();
            if (window != now && window != now - 1) return false;
            return MessageDigest.isEqual(
                    sign(sessionId, window).getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}