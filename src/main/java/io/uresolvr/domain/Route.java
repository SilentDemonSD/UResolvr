package io.uresolvr.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Core route entity — maps a secure token to an encrypted destination.
 *
 * <p>Destinations are NEVER stored in plaintext. The {@code encryptedDestination}
 * field contains AES-256-GCM (or ChaCha20-Poly1305) ciphertext. Decryption
 * occurs only at resolution time, in memory, never logged.
 */
@Table("routes")
public class Route {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    /** URL-safe token used for public resolution (e.g. /r/abc123XYZ) */
    @Column("token")
    private String token;

    /** HMAC signature of the token for tamper detection */
    @Column("token_signature")
    private String tokenSignature;

    /** AES-256-GCM encrypted destination URL (Base64-encoded ciphertext) */
    @Column("encrypted_destination")
    private String encryptedDestination;

    /** Identifier of the encryption key used (for key rotation support) */
    @Column("encryption_key_id")
    private String encryptionKeyId;

    /** Nonce/IV used for encryption (Base64-encoded) */
    @Column("nonce")
    private String nonce;

    @Column("status")
    private RouteStatus status;

    /** Total number of times this route has been resolved */
    @Column("resolve_count")
    private long resolveCount;

    /** Maximum allowed resolutions (null = unlimited) */
    @Column("max_resolves")
    private Integer maxResolves;

    /** When this route expires (null = never) */
    @Column("expires_at")
    private Instant expiresAt;

    /** Human-readable label for the route */
    @Column("label")
    private String label;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    // === Constructors ===

    public Route() {}

    public static Route create(UUID tenantId, String token, String tokenSignature,
                                String encryptedDestination, String encryptionKeyId,
                                String nonce, String label, Integer maxResolves,
                                Instant expiresAt) {
        Route route = new Route();
        route.tenantId = tenantId;
        route.token = token;
        route.tokenSignature = tokenSignature;
        route.encryptedDestination = encryptedDestination;
        route.encryptionKeyId = encryptionKeyId;
        route.nonce = nonce;
        route.label = label;
        route.status = RouteStatus.ACTIVE;
        route.resolveCount = 0;
        route.maxResolves = maxResolves;
        route.expiresAt = expiresAt;
        return route;
    }

    // === Domain Logic ===

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isMaxResolvesReached() {
        return maxResolves != null && resolveCount >= maxResolves;
    }

    public boolean isResolvable() {
        return status == RouteStatus.ACTIVE && !isExpired() && !isMaxResolvesReached();
    }

    public void incrementResolveCount() {
        this.resolveCount++;
    }

    public void revoke() {
        this.status = RouteStatus.REVOKED;
    }

    public void expire() {
        this.status = RouteStatus.EXPIRED;
    }

    // === Getters & Setters ===

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenSignature() { return tokenSignature; }
    public void setTokenSignature(String tokenSignature) { this.tokenSignature = tokenSignature; }

    public String getEncryptedDestination() { return encryptedDestination; }
    public void setEncryptedDestination(String encryptedDestination) { this.encryptedDestination = encryptedDestination; }

    public String getEncryptionKeyId() { return encryptionKeyId; }
    public void setEncryptionKeyId(String encryptionKeyId) { this.encryptionKeyId = encryptionKeyId; }

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public RouteStatus getStatus() { return status; }
    public void setStatus(RouteStatus status) { this.status = status; }

    public long getResolveCount() { return resolveCount; }
    public void setResolveCount(long resolveCount) { this.resolveCount = resolveCount; }

    public Integer getMaxResolves() { return maxResolves; }
    public void setMaxResolves(Integer maxResolves) { this.maxResolves = maxResolves; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
