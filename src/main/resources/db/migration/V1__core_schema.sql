-- =====================================================
-- UResolvr V1 Schema — Core Tables
-- Compatible with H2 (MODE=PostgreSQL) and PostgreSQL
-- =====================================================

-- Tenants (organizations / user accounts)
CREATE TABLE IF NOT EXISTS tenants (
    id              UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    api_key_hash    VARCHAR(128),
    api_key_prefix  VARCHAR(16),
    plan            VARCHAR(20) NOT NULL DEFAULT 'FREE',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    role            VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    max_routes      INT NOT NULL DEFAULT 100,
    max_resolves_per_day BIGINT NOT NULL DEFAULT 10000,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Routes (secure URI mappings)
CREATE TABLE IF NOT EXISTS routes (
    id                      UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    token                   VARCHAR(32) NOT NULL UNIQUE,
    token_signature         VARCHAR(64) NOT NULL,
    encrypted_destination   TEXT NOT NULL,
    encryption_key_id       VARCHAR(64) NOT NULL,
    nonce                   VARCHAR(32) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    resolve_count           BIGINT NOT NULL DEFAULT 0,
    max_resolves            INT,
    expires_at              TIMESTAMP WITH TIME ZONE,
    label                   VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_routes_tenant ON routes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_routes_status ON routes(status) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_routes_expires ON routes(expires_at) WHERE expires_at IS NOT NULL AND status = 'ACTIVE';

-- Route Policies (access control rules)
CREATE TABLE IF NOT EXISTS route_policies (
    id              UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    route_id        UUID NOT NULL UNIQUE REFERENCES routes(id) ON DELETE CASCADE,
    allowed_regions VARCHAR(500),
    allowed_devices VARCHAR(200),
    allowed_ips     TEXT,
    blocked_ips     TEXT,
    require_captcha BOOLEAN NOT NULL DEFAULT FALSE,
    one_time_use    BOOLEAN NOT NULL DEFAULT FALSE,
    trust_score_min SMALLINT NOT NULL DEFAULT 0,
    custom_rules    TEXT,
    require_auth    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Resolution Events (analytics — append-only)
CREATE TABLE IF NOT EXISTS resolution_events (
    id          UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    route_id    UUID,
    tenant_id   UUID,
    token       VARCHAR(32),
    resolved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_ip   VARCHAR(45),
    user_agent  TEXT,
    referer     TEXT,
    country     VARCHAR(3),
    device_type VARCHAR(20),
    trust_score SMALLINT DEFAULT 0,
    outcome     VARCHAR(30) NOT NULL,
    latency_ms  INT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_events_tenant_time ON resolution_events(tenant_id, resolved_at);
CREATE INDEX IF NOT EXISTS idx_events_route ON resolution_events(route_id);
CREATE INDEX IF NOT EXISTS idx_events_outcome ON resolution_events(outcome);

-- Audit Log (admin actions — append-only)
CREATE TABLE IF NOT EXISTS audit_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id     UUID,
    actor_id      UUID,
    action        VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32),
    resource_id   VARCHAR(64),
    details       TEXT,
    ip_address    VARCHAR(45),
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_tenant ON audit_log(tenant_id, created_at);
