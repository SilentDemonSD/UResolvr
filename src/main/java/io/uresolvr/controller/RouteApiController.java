package io.uresolvr.controller;

import io.uresolvr.dto.CreateRouteRequest;
import io.uresolvr.dto.ProblemDetail;
import io.uresolvr.dto.RouteResponse;
import io.uresolvr.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST API for route management (authenticated).
 */
@RestController
@RequestMapping("/api/v1/routes")
public class RouteApiController {

    private final RouteService routeService;

    public RouteApiController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RouteResponse> createRoute(@Valid @RequestBody CreateRouteRequest request,
                                            @AuthenticationPrincipal UUID tenantId,
                                            ServerHttpRequest httpRequest) {
        String baseUrl = getBaseUrl(httpRequest);
        return routeService.createRoute(tenantId, request, baseUrl);
    }

    @GetMapping
    public Flux<RouteResponse> listRoutes(@AuthenticationPrincipal UUID tenantId,
                                           ServerHttpRequest httpRequest) {
        return routeService.listRoutes(tenantId, getBaseUrl(httpRequest));
    }

    @GetMapping("/{id}")
    public Mono<RouteResponse> getRoute(@PathVariable UUID id,
                                         @AuthenticationPrincipal UUID tenantId,
                                         ServerHttpRequest httpRequest) {
        return routeService.getRoute(id, tenantId, getBaseUrl(httpRequest))
            .switchIfEmpty(Mono.error(new RouteNotFoundException()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> revokeRoute(@PathVariable UUID id,
                                   @AuthenticationPrincipal UUID tenantId) {
        return routeService.revokeRoute(id, tenantId);
    }

    private String getBaseUrl(ServerHttpRequest request) {
        // Respect X-Forwarded headers for proxy setups
        String host = request.getHeaders().getFirst("X-Forwarded-Host");
        String proto = request.getHeaders().getFirst("X-Forwarded-Proto");
        if (host != null) {
            return (proto != null ? proto : "https") + "://" + host;
        }
        return request.getURI().getScheme() + "://" + request.getURI().getAuthority();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class RouteNotFoundException extends RuntimeException {
        RouteNotFoundException() { super("Route not found"); }
    }
}
