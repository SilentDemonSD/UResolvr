package io.uresolvr.service;

import io.uresolvr.crypto.CipherResult;
import io.uresolvr.crypto.CryptoProvider;
import io.uresolvr.crypto.TokenGenerator;
import io.uresolvr.domain.Route;
import io.uresolvr.domain.RoutePolicy;
import io.uresolvr.domain.RouteStatus;
import io.uresolvr.dto.CreateRouteRequest;
import io.uresolvr.dto.RouteResponse;
import io.uresolvr.repository.RoutePolicyRepository;
import io.uresolvr.repository.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Route management — create, update, revoke, list routes.
 * All destination URLs are encrypted before storage.
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;
    private final RoutePolicyRepository policyRepository;
    private final CryptoProvider cryptoProvider;
    private final TokenGenerator tokenGenerator;

    public RouteService(RouteRepository routeRepository,
                        RoutePolicyRepository policyRepository,
                        CryptoProvider cryptoProvider,
                        TokenGenerator tokenGenerator) {
        this.routeRepository = routeRepository;
        this.policyRepository = policyRepository;
        this.cryptoProvider = cryptoProvider;
        this.tokenGenerator = tokenGenerator;
    }

    /**
     * Create a new secure route. The destination URL is encrypted
     * before it ever touches the database.
     */
    @Transactional
    public Mono<RouteResponse> createRoute(UUID tenantId, CreateRouteRequest request, String baseUrl) {
        // 1. Generate unique token
        String token = tokenGenerator.generateToken();
        String signature = tokenGenerator.sign(token);

        // 2. Encrypt the destination URL — it's NEVER stored in plaintext
        CipherResult cipher = cryptoProvider.encrypt(
            request.destination().getBytes(StandardCharsets.UTF_8)
        );

        // 3. Calculate expiry
        Instant expiresAt = null;
        if (request.ttlHours() != null) {
            expiresAt = Instant.now().plus(request.ttlHours(), ChronoUnit.HOURS);
        }
        if (request.oneTimeUse() && request.maxResolves() == null) {
            // One-time use implies max 1 resolve
        }

        Integer maxResolves = request.maxResolves();
        if (request.oneTimeUse() && maxResolves == null) {
            maxResolves = 1;
        }

        // 4. Build route entity
        Route route = Route.create(
            tenantId, token, signature,
            cipher.ciphertext(), cipher.keyId(), cipher.nonce(),
            request.label(), maxResolves, expiresAt
        );

        // 5. Save route then create policy
        return routeRepository.save(route)
            .flatMap(saved -> {
                RoutePolicy policy = new RoutePolicy();
                policy.setRouteId(saved.getId());
                policy.setOneTimeUse(request.oneTimeUse());
                policy.setTrustScoreMin(request.trustScoreMin() != null ? request.trustScoreMin() : 0);
                policy.setAllowedRegions(request.allowedRegions());
                policy.setAllowedDevices(request.allowedDevices());
                policy.setRequireAuth(request.requireAuth());
                return policyRepository.save(policy)
                    .thenReturn(toResponse(saved, policy, baseUrl));
            })
            .doOnSuccess(r -> log.info("Route created: token={}, tenant={}", token, tenantId));
    }

    public Flux<RouteResponse> listRoutes(UUID tenantId, String baseUrl) {
        return routeRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
            .flatMap(route -> policyRepository.findByRouteId(route.getId())
                .map(policy -> toResponse(route, policy, baseUrl))
                .defaultIfEmpty(toResponse(route, null, baseUrl))
            );
    }

    public Mono<RouteResponse> getRoute(UUID routeId, UUID tenantId, String baseUrl) {
        return routeRepository.findById(routeId)
            .filter(r -> r.getTenantId().equals(tenantId))
            .flatMap(route -> policyRepository.findByRouteId(route.getId())
                .map(policy -> toResponse(route, policy, baseUrl))
                .defaultIfEmpty(toResponse(route, null, baseUrl))
            );
    }

    @Transactional
    public Mono<Void> revokeRoute(UUID routeId, UUID tenantId) {
        return routeRepository.findById(routeId)
            .filter(r -> r.getTenantId().equals(tenantId))
            .flatMap(route -> {
                route.revoke();
                return routeRepository.save(route);
            })
            .doOnSuccess(r -> log.info("Route revoked: id={}", routeId))
            .then();
    }

    public Mono<Long> countRoutes(UUID tenantId) {
        return routeRepository.countByTenantId(tenantId);
    }

    private RouteResponse toResponse(Route route, RoutePolicy policy, String baseUrl) {
        String resolveUrl = baseUrl + "/r/" + route.getToken();
        RouteResponse.PolicySummary policySummary = null;
        if (policy != null) {
            policySummary = new RouteResponse.PolicySummary(
                policy.isOneTimeUse(),
                policy.isRequireAuth(),
                policy.isRequireCaptcha(),
                policy.getTrustScoreMin(),
                policy.getAllowedRegions(),
                policy.getAllowedDevices()
            );
        }
        return new RouteResponse(
            route.getId(), route.getToken(), resolveUrl, route.getLabel(),
            route.getStatus(), route.getResolveCount(), route.getMaxResolves(),
            route.getExpiresAt(), route.getCreatedAt(), policySummary
        );
    }
}
