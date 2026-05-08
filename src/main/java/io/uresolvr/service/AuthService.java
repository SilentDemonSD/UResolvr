package io.uresolvr.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.uresolvr.domain.Tenant;
import io.uresolvr.dto.AuthResponse;
import io.uresolvr.dto.LoginRequest;
import io.uresolvr.dto.SetupRequest;
import io.uresolvr.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey jwtKey;
    private final long jwtExpirationMs;

    public AuthService(TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${uresolvr.security.jwt.secret:}") String jwtSecret,
                       @Value("${uresolvr.security.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;

        if (jwtSecret == null || jwtSecret.isBlank()) {
            byte[] generated = new byte[64];
            new SecureRandom().nextBytes(generated);
            jwtSecret = Base64.getEncoder().encodeToString(generated);
        }
        this.jwtKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = expirationMinutes * 60 * 1000;
    }

    /**
     * First-run setup — creates the admin tenant.
     */
    public Mono<AuthResponse> setup(SetupRequest request) {
        return tenantRepository.count()
            .flatMap(count -> {
                if (count > 0) {
                    return Mono.error(new IllegalStateException("Setup already completed"));
                }
                String hash = passwordEncoder.encode(request.password());
                Tenant tenant = Tenant.createDefault(request.name(), request.email(), hash);
                return tenantRepository.save(tenant)
                    .map(saved -> {
                        log.info("Admin tenant created: {}", saved.getEmail());
                        return generateTokens(saved);
                    });
            });
    }

    /**
     * Authenticate with email + password.
     */
    public Mono<AuthResponse> login(LoginRequest request) {
        return tenantRepository.findByEmail(request.email())
            .filter(tenant -> passwordEncoder.matches(request.password(), tenant.getPasswordHash()))
            .map(this::generateTokens)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid email or password")));
    }

    /**
     * Check if first-run setup is needed.
     */
    public Mono<Boolean> isSetupRequired() {
        return tenantRepository.count().map(count -> count == 0);
    }

    /**
     * Validate a JWT token and return the tenant ID.
     */
    public Mono<UUID> validateToken(String token) {
        try {
            var claims = Jwts.parser()
                .verifyWith(jwtKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            UUID tenantId = UUID.fromString(claims.getSubject());
            return Mono.just(tenantId);
        } catch (Exception e) {
            return Mono.error(new SecurityException("Invalid token"));
        }
    }

    public Mono<Tenant> getTenantById(UUID id) {
        return tenantRepository.findById(id);
    }

    private AuthResponse generateTokens(Tenant tenant) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        String accessToken = Jwts.builder()
            .subject(tenant.getId().toString())
            .claim("email", tenant.getEmail())
            .claim("role", tenant.getRole().name())
            .claim("plan", tenant.getPlan().name())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(jwtKey)
            .compact();

        // Refresh token — longer lived
        String refreshToken = Jwts.builder()
            .subject(tenant.getId().toString())
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(new Date(now.getTime() + 30L * 24 * 60 * 60 * 1000))
            .signWith(jwtKey)
            .compact();

        return AuthResponse.bearer(accessToken, refreshToken, jwtExpirationMs / 1000);
    }
}
