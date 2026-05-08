package io.uresolvr.crypto;

/**
 * Abstraction for symmetric encryption — allows swapping algorithms
 * (AES-GCM, ChaCha20, future PQC) without touching business logic.
 */
public interface CryptoProvider {

    /** Encrypt plaintext using the current active key. */
    CipherResult encrypt(byte[] plaintext);

    /** Decrypt ciphertext using the key identified by keyId. */
    byte[] decrypt(String ciphertext, String nonce, String keyId);

    /** Get the algorithm name (e.g. "AES-256-GCM"). */
    String algorithm();
}
