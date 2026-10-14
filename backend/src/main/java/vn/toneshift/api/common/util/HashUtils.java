package vn.toneshift.api.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates opaque tokens and hashes them for storage.
 *
 * <p>Refresh and password reset tokens are persisted as a SHA-256 hex digest, so a database dump
 * cannot be replayed. Plain SHA-256 is right here, unlike for passwords: the inputs are
 * 256 bits of entropy, so there is nothing to brute-force.
 */
public final class HashUtils {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Utility class, not instantiable.
    private HashUtils() {
    }

    /**
     * Generates a URL-safe random token with 256 bits of entropy.
     *
     * @return the raw token, to be handed out once and never stored
     */
    public static String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashes a token for storage and lookup.
     *
     * @param value the raw token
     * @return the lowercase SHA-256 hex digest, 64 characters long
     */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng trên JVM này", e);
        }
    }
}
