package io.uresolvr.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Route access policy — controls WHO can resolve a route and under what conditions.
 */
@Table("route_policies")
public class RoutePolicy {

    @Id
    private UUID id;

    @Column("route_id")
    private UUID routeId;

    /** Comma-separated ISO country codes (null = all allowed) */
    @Column("allowed_regions")
    private String allowedRegions;

    /** Comma-separated device types: DESKTOP, MOBILE, TABLET (null = all) */
    @Column("allowed_devices")
    private String allowedDevices;

    /** Comma-separated allowed IP CIDRs (null = all) */
    @Column("allowed_ips")
    private String allowedIps;

    /** Comma-separated blocked IP CIDRs */
    @Column("blocked_ips")
    private String blockedIps;

    /** Require CAPTCHA verification before resolving */
    @Column("require_captcha")
    private boolean requireCaptcha;

    /** Route self-destructs after single use */
    @Column("one_time_use")
    private boolean oneTimeUse;

    /** Minimum trust score (0-100) required to resolve */
    @Column("trust_score_min")
    private int trustScoreMin;

    /** Custom rules as JSON for extensibility */
    @Column("custom_rules")
    private String customRules;

    @Column("require_auth")
    private boolean requireAuth;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    public RoutePolicy() {}

    // === Getters & Setters ===

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRouteId() { return routeId; }
    public void setRouteId(UUID routeId) { this.routeId = routeId; }

    public String getAllowedRegions() { return allowedRegions; }
    public void setAllowedRegions(String allowedRegions) { this.allowedRegions = allowedRegions; }

    public String getAllowedDevices() { return allowedDevices; }
    public void setAllowedDevices(String allowedDevices) { this.allowedDevices = allowedDevices; }

    public String getAllowedIps() { return allowedIps; }
    public void setAllowedIps(String allowedIps) { this.allowedIps = allowedIps; }

    public String getBlockedIps() { return blockedIps; }
    public void setBlockedIps(String blockedIps) { this.blockedIps = blockedIps; }

    public boolean isRequireCaptcha() { return requireCaptcha; }
    public void setRequireCaptcha(boolean requireCaptcha) { this.requireCaptcha = requireCaptcha; }

    public boolean isOneTimeUse() { return oneTimeUse; }
    public void setOneTimeUse(boolean oneTimeUse) { this.oneTimeUse = oneTimeUse; }

    public int getTrustScoreMin() { return trustScoreMin; }
    public void setTrustScoreMin(int trustScoreMin) { this.trustScoreMin = trustScoreMin; }

    public String getCustomRules() { return customRules; }
    public void setCustomRules(String customRules) { this.customRules = customRules; }

    public boolean isRequireAuth() { return requireAuth; }
    public void setRequireAuth(boolean requireAuth) { this.requireAuth = requireAuth; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
