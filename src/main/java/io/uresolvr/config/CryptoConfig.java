package io.uresolvr.config;

import io.uresolvr.crypto.AesGcmCryptoProvider;
import io.uresolvr.crypto.CryptoProvider;
import io.uresolvr.crypto.TokenGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Configuration
public class CryptoConfig {

    private static final Logger log = LoggerFactory.getLogger(CryptoConfig.class);

    @Value("${uresolvr.crypto.master-key:}")
    private String masterKey;

    @Value("${uresolvr.token.length:10}")
    private int tokenLength;

    @Value("${uresolvr.security.jwt.secret:}")
    private String jwtSecret;

    @Bean
    public CryptoProvider cryptoProvider() {
        String key = masterKey;
        String keyId = "key-1";

        if (key == null || key.isBlank()) {
            // Auto-generate a master key for first-run convenience
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            key = Base64.getEncoder().encodeToString(generated);
            log.warn("═══════════════════════════════════════════════════════════");
            log.warn("  No URESOLVR_MASTER_KEY set — auto-generated for dev use.");
            log.warn("  Set URESOLVR_MASTER_KEY env var for production!");
            log.warn("  Generated key: {}", key);
            log.warn("═══════════════════════════════════════════════════════════");
            keyId = "auto-" + UUID.randomUUID().toString().substring(0, 8);
        }

        return new AesGcmCryptoProvider(key, keyId);
    }

    @Bean
    public TokenGenerator tokenGenerator() {
        String secret = jwtSecret;
        if (secret == null || secret.isBlank()) {
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            secret = Base64.getEncoder().encodeToString(generated);
            log.warn("No URESOLVR_JWT_SECRET set — auto-generated for dev use.");
        }
        return new TokenGenerator(secret, tokenLength);
    }
}
