package io.uresolvr.dto;

import io.uresolvr.domain.RouteStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Route response — never exposes the actual destination URL.
 */
public record RouteResponse(
    UUID id,
    String token,
    String resolveUrl,
    String label,
    RouteStatus status,
    long resolveCount,
    Integer maxResolves,
    Instant expiresAt,
    Instant createdAt,
    PolicySummary policy
) {
    public record PolicySummary(
        boolean oneTimeUse,
        boolean requireAuth,
        boolean requireCaptcha,
        int trustScoreMin,
        String allowedRegions,
        String allowedDevices
    ) {}
}
