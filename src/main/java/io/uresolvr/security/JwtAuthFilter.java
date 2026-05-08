package io.uresolvr.security;

import io.uresolvr.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT authentication filter — extracts Bearer token from Authorization header,
 * validates it, and sets the security context for downstream handlers.
 */
@Component
public class JwtAuthFilter implements WebFilter {

    private final AuthService authService;

    public JwtAuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        return authService.validateToken(token)
            .flatMap(tenantId -> authService.getTenantById(tenantId)
                .flatMap(tenant -> {
                    var auth = new UsernamePasswordAuthenticationToken(
                        tenant.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + tenant.getRole().name()))
                    );
                    return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                })
            )
            .onErrorResume(e -> chain.filter(exchange));
    }
}
