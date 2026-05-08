package io.uresolvr.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant — represents an organization or user account that owns routes.
 */
@Table("tenants")
public class Tenant {

    @Id
    private UUID id;

    @Column("name")
    private String name;

    @Column("email")
    private String email;

    /** BCrypt-hashed password */
    @Column("password_hash")
    private String passwordHash;

    /** SHA-256 hash of the API key */
    @Column("api_key_hash")
    private String apiKeyHash;

    /** Prefix of API key for display (first 8 chars) */
    @Column("api_key_prefix")
    private String apiKeyPrefix;

    @Column("plan")
    private TenantPlan plan;

    @Column("status")
    private TenantStatus status;

    @Column("role")
    private TenantRole role;

    @Column("max_routes")
    private int maxRoutes;

    @Column("max_resolves_per_day")
    private long maxResolvesPerDay;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    public Tenant() {}

    public static Tenant createDefault(String name, String email, String passwordHash) {
        Tenant t = new Tenant();
        t.name = name;
        t.email = email;
        t.passwordHash = passwordHash;
        t.plan = TenantPlan.FREE;
        t.status = TenantStatus.ACTIVE;
        t.role = TenantRole.ADMIN;
        t.maxRoutes = 100;
        t.maxResolvesPerDay = 10_000;
        return t;
    }

    // === Getters & Setters ===

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; }

    public String getApiKeyPrefix() { return apiKeyPrefix; }
    public void setApiKeyPrefix(String apiKeyPrefix) { this.apiKeyPrefix = apiKeyPrefix; }

    public TenantPlan getPlan() { return plan; }
    public void setPlan(TenantPlan plan) { this.plan = plan; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public TenantRole getRole() { return role; }
    public void setRole(TenantRole role) { this.role = role; }

    public int getMaxRoutes() { return maxRoutes; }
    public void setMaxRoutes(int maxRoutes) { this.maxRoutes = maxRoutes; }

    public long getMaxResolvesPerDay() { return maxResolvesPerDay; }
    public void setMaxResolvesPerDay(long maxResolvesPerDay) { this.maxResolvesPerDay = maxResolvesPerDay; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
