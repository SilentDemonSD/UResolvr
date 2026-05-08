package io.uresolvr.repository;

import io.uresolvr.domain.ResolutionEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface ResolutionEventRepository extends R2dbcRepository<ResolutionEvent, UUID> {

    Flux<ResolutionEvent> findByTenantIdAndResolvedAtBetweenOrderByResolvedAtDesc(
        UUID tenantId, Instant from, Instant to);

    Flux<ResolutionEvent> findByRouteIdOrderByResolvedAtDesc(UUID routeId);

    Mono<Long> countByTenantIdAndResolvedAtAfter(UUID tenantId, Instant after);

    @Query("SELECT outcome, COUNT(*) as cnt FROM resolution_events " +
           "WHERE tenant_id = :tenantId AND resolved_at > :after " +
           "GROUP BY outcome")
    Flux<OutcomeCount> countByOutcome(UUID tenantId, Instant after);

    /** Projection for outcome aggregation. */
    interface OutcomeCount {
        String getOutcome();
        long getCnt();
    }
}
