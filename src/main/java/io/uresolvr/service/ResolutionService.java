package io.uresolvr.service;

import io.uresolvr.crypto.CryptoProvider;
import io.uresolvr.domain.*;
import io.uresolvr.repository.ResolutionEventRepository;
import io.uresolvr.repository.RoutePolicyRepository;
import io.uresolvr.repository.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;

/**
 * The hot path — resolves tokens to destination URLs.
 *
 * <p>Design priorities: minimal latency, non-blocking, async analytics.
 * The destination is decrypted in-memory and NEVER logged or cached in plaintext.
 */
@Service
public class ResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ResolutionService.class);

    private final RouteRepository routeRepository;
    private final RoutePolicyRepository policyRepository;
    private final ResolutionEventRepository eventRepository;
    private final CryptoProvider cryptoProvider;

    public ResolutionService(RouteRepository routeRepository,
                              RoutePolicyRepository policyRepository,
                              ResolutionEventRepository eventRepository,
                              CryptoProvider cryptoProvider) {
        this.routeRepository = routeRepository;
        this.policyRepository = policyRepository;
        this.eventRepository = eventRepository;
        this.cryptoProvider = cryptoProvider;
    }

    /**
     * Resolve a token to its destination URL.
     *
     * @return Decrypted destination URL, or error via Mono.error
     */
    public Mono<ResolutionResult> resolve(String token, ResolutionContext ctx) {
        long startNanos = System.nanoTime();

        return routeRepository.findByToken(token)
            .switchIfEmpty(Mono.defer(() -> {
                recordEvent(null, null, token, ctx, ResolutionOutcome.NOT_FOUND, startNanos);
                return Mono.error(new ResolutionException(ResolutionOutcome.NOT_FOUND, "Route not found"));
            }))
            .flatMap(route -> {
                // Check basic route validity
                if (route.getStatus() == RouteStatus.REVOKED) {
                    recordEvent(route.getId(), route.getTenantId(), token, ctx, ResolutionOutcome.BLOCKED_REVOKED, startNanos);
                    return Mono.error(new ResolutionException(ResolutionOutcome.BLOCKED_REVOKED, "Route has been revoked"));
                }
                if (route.isExpired()) {
                    recordEvent(route.getId(), route.getTenantId(), token, ctx, ResolutionOutcome.BLOCKED_EXPIRED, startNanos);
                    return Mono.error(new ResolutionException(ResolutionOutcome.BLOCKED_EXPIRED, "Route has expired"));
                }
                if (route.isMaxResolvesReached()) {
                    recordEvent(route.getId(), route.getTenantId(), token, ctx, ResolutionOutcome.BLOCKED_MAX_RESOLVES, startNanos);
                    return Mono.error(new ResolutionException(ResolutionOutcome.BLOCKED_MAX_RESOLVES, "Maximum resolves reached"));
                }

                // Check policy
                return policyRepository.findByRouteId(route.getId())
                    .defaultIfEmpty(new RoutePolicy())
                    .flatMap(policy -> {
                        // Policy checks
                        ResolutionOutcome policyViolation = checkPolicy(policy, ctx);
                        if (policyViolation != null) {
                            recordEvent(route.getId(), route.getTenantId(), token, ctx, policyViolation, startNanos);
                            return Mono.error(new ResolutionException(policyViolation, "Access denied by policy"));
                        }

                        // Decrypt destination — in memory only, never logged
                        String destination;
                        try {
                            byte[] plaintext = cryptoProvider.decrypt(
                                route.getEncryptedDestination(),
                                route.getNonce(),
                                route.getEncryptionKeyId()
                            );
                            destination = new String(plaintext, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            log.error("Decryption failed for route {}", route.getId(), e);
                            return Mono.error(new ResolutionException(ResolutionOutcome.ERROR, "Resolution failed"));
                        }

                        // Increment resolve count atomically (fire-and-forget)
                        routeRepository.incrementResolveCount(route.getId())
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();

                        // Record analytics event asynchronously
                        recordEvent(route.getId(), route.getTenantId(), token, ctx, ResolutionOutcome.SUCCESS, startNanos);

                        return Mono.just(new ResolutionResult(destination, route.getId()));
                    });
            });
    }

    private ResolutionOutcome checkPolicy(RoutePolicy policy, ResolutionContext ctx) {
        if (policy.getTrustScoreMin() > 0 && ctx.trustScore() < policy.getTrustScoreMin()) {
            return ResolutionOutcome.BLOCKED_TRUST_SCORE;
        }
        if (policy.getAllowedRegions() != null && !policy.getAllowedRegions().isBlank()) {
            if (ctx.country() != null && !policy.getAllowedRegions().contains(ctx.country())) {
                return ResolutionOutcome.BLOCKED_REGION;
            }
        }
        if (policy.getAllowedDevices() != null && !policy.getAllowedDevices().isBlank()) {
            if (ctx.deviceType() != null && !policy.getAllowedDevices().toUpperCase().contains(ctx.deviceType().toUpperCase())) {
                return ResolutionOutcome.BLOCKED_DEVICE;
            }
        }
        if (policy.getBlockedIps() != null && !policy.getBlockedIps().isBlank()) {
            if (ctx.clientIp() != null && policy.getBlockedIps().contains(ctx.clientIp())) {
                return ResolutionOutcome.BLOCKED_IP;
            }
        }
        return null; // No violation
    }

    /** Fire-and-forget async event recording — never blocks resolution. */
    private void recordEvent(java.util.UUID routeId, java.util.UUID tenantId, String token,
                              ResolutionContext ctx, ResolutionOutcome outcome, long startNanos) {
        int latencyMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
        ResolutionEvent event = ResolutionEvent.of(
            routeId, tenantId, token,
            ctx.clientIp(), ctx.userAgent(), ctx.referer(),
            ctx.country(), ctx.deviceType(), ctx.trustScore(),
            outcome, latencyMs
        );
        eventRepository.save(event)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                saved -> {},
                err -> log.warn("Failed to record resolution event: {}", err.getMessage())
            );
    }

    public record ResolutionResult(String destination, java.util.UUID routeId) {}

    public record ResolutionContext(
        String clientIp,
        String userAgent,
        String referer,
        String country,
        String deviceType,
        int trustScore
    ) {}

    public static class ResolutionException extends RuntimeException {
        private final ResolutionOutcome outcome;

        public ResolutionException(ResolutionOutcome outcome, String message) {
            super(message);
            this.outcome = outcome;
        }

        public ResolutionOutcome getOutcome() { return outcome; }
    }
}
