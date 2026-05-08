package io.uresolvr.repository;

import io.uresolvr.domain.Route;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RouteRepository extends R2dbcRepository<Route, UUID> {

    /** Primary lookup — the hot path for resolution. */
    Mono<Route> findByToken(String token);

    /** All routes for a tenant (paginated via Spring). */
    Flux<Route> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /** Count routes per tenant (for quota enforcement). */
    Mono<Long> countByTenantId(UUID tenantId);

    /** Atomically increment resolve count — avoids race conditions. */
    @Modifying
    @Query("UPDATE routes SET resolve_count = resolve_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> incrementResolveCount(UUID id);

    /** Bulk expire routes past their expiration. */
    @Modifying
    @Query("UPDATE routes SET status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP " +
           "WHERE status = 'ACTIVE' AND expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP")
    Mono<Integer> expireOverdueRoutes();

    /** Count active routes for a tenant. */
    Mono<Long> countByTenantIdAndStatus(UUID tenantId, String status);
}
