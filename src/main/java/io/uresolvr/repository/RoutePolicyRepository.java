package io.uresolvr.repository;

import io.uresolvr.domain.RoutePolicy;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoutePolicyRepository extends R2dbcRepository<RoutePolicy, UUID> {

    Mono<RoutePolicy> findByRouteId(UUID routeId);
}
