package io.uresolvr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.URL;

/**
 * Request to create a new secure route.
 */
public record CreateRouteRequest(
    @NotBlank(message = "Destination URL is required")
    @URL(message = "Must be a valid URL")
    String destination,

    /** Human-readable label (optional) */
    String label,

    /** Max number of resolutions (null = unlimited) */
    @Positive(message = "Max resolves must be positive")
    Integer maxResolves,

    /** Hours until expiry (null = never) */
    @Positive(message = "TTL must be positive")
    Integer ttlHours,

    /** One-time use — self-destructs after single resolution */
    boolean oneTimeUse,

    /** Require minimum trust score (0-100) */
    Integer trustScoreMin,

    /** Comma-separated allowed country codes */
    String allowedRegions,

    /** Comma-separated allowed device types */
    String allowedDevices,

    /** Require authentication to resolve */
    boolean requireAuth
) {}
