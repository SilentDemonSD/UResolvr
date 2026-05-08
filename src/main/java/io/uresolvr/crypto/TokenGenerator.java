package io.uresolvr.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates URL-safe tokens and HMAC signatures for tamper detection.
 *
 * <p>Tokens use Base62 alphabet (a-z, A-Z, 0-9) for maximum URL compatibility.
 * Each token is also HMAC-SHA256 signed to prevent enumeration attacks —
 * invalid tokens are rejected in constant time without a DB lookup.
 */
public class TokenGenerator {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] hmacKey;
    private final int tokenLength;

    public TokenGenerator(String hmacSecret, int tokenLength) {
        this.hmacKey = hmacSecret.getBytes(StandardCharsets.UTF_8);
        this.tokenLength = tokenLength;
    }

    /**
     * Generate a cryptographically random Base62 token.
     * 10-char Base62 = 62^10 ≈ 8.4 × 10^17 combinations — infeasible to enumerate.
     */
    public String generateToken() {
        StringBuilder sb = new StringBuilder(tokenLength);
        for (int i = 0; i < tokenLength; i++) {
            sb.append(BASE62.charAt(RANDOM.nextInt(BASE62.length())));
        }
        return sb.toString();
    }

    /**
     * HMAC-SHA256 signature of a token — stored alongside the token.
     * Allows early rejection of forged tokens without DB hit.
     */
    public String sign(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            byte[] sig = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (Exception e) {
            throw new CryptoException("HMAC signing failed", e);
        }
    }

    /**
     * Constant-time signature verification — prevents timing attacks.
     */
    public boolean verify(String token, String expectedSignature) {
        String actual = sign(token);
        if (actual.length() != expectedSignature.length()) return false;
        int result = 0;
        for (int i = 0; i < actual.length(); i++) {
            result |= actual.charAt(i) ^ expectedSignature.charAt(i);
        }
        return result == 0;
    }
}
