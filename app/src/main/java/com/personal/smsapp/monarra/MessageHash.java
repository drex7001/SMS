package com.personal.smsapp.monarra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Identifies one SMS across live capture and backfill, so the same message never enters the feed
 * twice: hex(SHA-256(upper(trim(sender)) + "|" + sent_at_ms + "|" + body)), UTF-8.
 */
public final class MessageHash {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String of(String sender, long sentAtMs, String body) {
        String input = sender.trim().toUpperCase(Locale.ROOT) + "|" + sentAtMs + "|" + body;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            char[] out = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                out[i * 2]     = HEX[(digest[i] >> 4) & 0xF];
                out[i * 2 + 1] = HEX[digest[i] & 0xF];
            }
            return new String(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    private MessageHash() {}
}
