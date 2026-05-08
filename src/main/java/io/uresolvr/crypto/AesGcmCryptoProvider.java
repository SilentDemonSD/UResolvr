package io.uresolvr.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AES-256-GCM encryption provider.
 *
 * <p>Why AES-GCM: Hardware-accelerated via AES-NI on modern CPUs,
 * authenticated encryption (integrity + confidentiality), industry standard.
 * 12-byte random nonce, 128-bit auth tag.
 */
public class AesGcmCryptoProvider implements CryptoProvider {

    private static final Logger log = LoggerFactory.getLogger(AesGcmCryptoProvider.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_NONCE_LENGTH = 12;  // bytes
    private static final int GCM_TAG_LENGTH = 128;    // bits

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SecretKey> keyStore = new ConcurrentHashMap<>();
    private volatile String currentKeyId;

    public AesGcmCryptoProvider(String masterKeyBase64, String keyId) {
        byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256 requires a 256-bit (32-byte) key, got " + keyBytes.length);
        }
        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        this.keyStore.put(keyId, key);
        this.currentKeyId = keyId;
        log.info("AES-256-GCM crypto provider initialized with key [{}]", keyId);
    }

    @Override
    public CipherResult encrypt(byte[] plaintext) {
        try {
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.get(currentKeyId), spec);

            byte[] ciphertext = cipher.doFinal(plaintext);

            return new CipherResult(
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(nonce),
                currentKeyId
            );
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    @Override
    public byte[] decrypt(String ciphertextBase64, String nonceBase64, String keyId) {
        try {
            SecretKey key = keyStore.get(keyId);
            if (key == null) {
                throw new CryptoException("Unknown key ID: " + keyId);
            }

            byte[] ciphertext = Base64.getDecoder().decode(ciphertextBase64);
            byte[] nonce = Base64.getDecoder().decode(nonceBase64);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            return cipher.doFinal(ciphertext);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("Decryption failed — possible tampering", e);
        }
    }

    @Override
    public String algorithm() {
        return "AES-256-GCM";
    }

    /** Register an additional key (for rotation — old keys stay for decryption). */
    public void addKey(String keyId, String keyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        keyStore.put(keyId, new SecretKeySpec(keyBytes, "AES"));
        log.info("Added encryption key [{}]", keyId);
    }

    /** Rotate to a new active key. Old key remains for decryption. */
    public void rotateToKey(String keyId) {
        if (!keyStore.containsKey(keyId)) {
            throw new IllegalArgumentException("Key not found: " + keyId);
        }
        this.currentKeyId = keyId;
        log.info("Rotated active encryption key to [{}]", keyId);
    }
}
