package io.uresolvr.crypto;

/**
 * Result of an encryption operation — contains everything needed to decrypt later.
 */
public record CipherResult(
    /** Base64-encoded ciphertext */
    String ciphertext,
    /** Base64-encoded nonce/IV */
    String nonce,
    /** Identifier of the key used (for key rotation) */
    String keyId
) {}
