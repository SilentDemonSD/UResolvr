package io.uresolvr.crypto;

/**
 * Unchecked exception for cryptographic operation failures.
 */
public class CryptoException extends RuntimeException {
    public CryptoException(String message) {
        super(message);
    }
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
