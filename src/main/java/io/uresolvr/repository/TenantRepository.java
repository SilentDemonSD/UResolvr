package io.uresolvr.repository;

import io.uresolvr.domain.Tenant;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TenantRepository extends R2dbcRepository<Tenant, UUID> {

    Mono<Tenant> findByEmail(String email);

    Mono<Tenant> findByApiKeyHash(String apiKeyHash);

    Mono<Boolean> existsByEmail(String email);
}
