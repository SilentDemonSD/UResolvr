package io.uresolvr.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable resolution event — written to DB asynchronously for analytics.
 * Never blocks the resolution hot path.
 */
@Table("resolution_events")
public class ResolutionEvent {

    @Id
    private UUID id;

    @Column("route_id")
    private UUID routeId;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("token")
    private String token;

    @Column("resolved_at")
    private Instant resolvedAt;

    @Column("client_ip")
    private String clientIp;

    @Column("user_agent")
    private String userAgent;

    @Column("referer")
    private String referer;

    @Column("country")
    private String country;

    @Column("device_type")
    private String deviceType;

    @Column("trust_score")
    private int trustScore;

    @Column("outcome")
    private ResolutionOutcome outcome;

    @Column("latency_ms")
    private int latencyMs;

    public ResolutionEvent() {}

    public static ResolutionEvent of(UUID routeId, UUID tenantId, String token,
                                      String clientIp, String userAgent, String referer,
                                      String country, String deviceType, int trustScore,
                                      ResolutionOutcome outcome, int latencyMs) {
        ResolutionEvent e = new ResolutionEvent();
        e.routeId = routeId;
        e.tenantId = tenantId;
        e.token = token;
        e.resolvedAt = Instant.now();
        e.clientIp = clientIp;
        e.userAgent = userAgent;
        e.referer = referer;
        e.country = country;
        e.deviceType = deviceType;
        e.trustScore = trustScore;
        e.outcome = outcome;
        e.latencyMs = latencyMs;
        return e;
    }

    // === Getters ===
    public UUID getId() { return id; }
    public UUID getRouteId() { return routeId; }
    public UUID getTenantId() { return tenantId; }
    public String getToken() { return token; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getClientIp() { return clientIp; }
    public String getUserAgent() { return userAgent; }
    public String getReferer() { return referer; }
    public String getCountry() { return country; }
    public String getDeviceType() { return deviceType; }
    public int getTrustScore() { return trustScore; }
    public ResolutionOutcome getOutcome() { return outcome; }
    public int getLatencyMs() { return latencyMs; }

    public void setId(UUID id) { this.id = id; }
}
