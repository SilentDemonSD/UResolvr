package io.uresolvr.controller;

import io.uresolvr.service.ResolutionService;
import io.uresolvr.service.ResolutionService.ResolutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * The public resolution endpoint — the HOT PATH.
 *
 * <p>{@code GET /r/{token}} resolves a token to its encrypted destination
 * and returns a 302 redirect. No authentication required. Ultra-low latency.
 */
@RestController
public class ResolutionController {

    private static final Logger log = LoggerFactory.getLogger(ResolutionController.class);

    private final ResolutionService resolutionService;

    public ResolutionController(ResolutionService resolutionService) {
        this.resolutionService = resolutionService;
    }

    @GetMapping("/r/{token}")
    public Mono<Void> resolve(@PathVariable String token, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ResolutionContext ctx = buildContext(request);

        return resolutionService.resolve(token, ctx)
            .flatMap(result -> {
                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                exchange.getResponse().getHeaders().setLocation(URI.create(result.destination()));
                // Security headers — prevent caching of redirect
                exchange.getResponse().getHeaders().setCacheControl("no-store, no-cache, must-revalidate");
                exchange.getResponse().getHeaders().setPragma("no-cache");
                exchange.getResponse().getHeaders().set("X-Robots-Tag", "noindex, nofollow");
                exchange.getResponse().getHeaders().set("Referrer-Policy", "no-referrer");
                return exchange.getResponse().setComplete();
            })
            .onErrorResume(ResolutionService.ResolutionException.class, e -> {
                HttpStatus status = switch (e.getOutcome()) {
                    case NOT_FOUND -> HttpStatus.NOT_FOUND;
                    case BLOCKED_RATE_LIMIT -> HttpStatus.TOO_MANY_REQUESTS;
                    default -> HttpStatus.FORBIDDEN;
                };
                exchange.getResponse().setStatusCode(status);
                return exchange.getResponse().setComplete();
            });
    }

    private ResolutionContext buildContext(ServerHttpRequest request) {
        String clientIp = extractClientIp(request);
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        String referer = request.getHeaders().getFirst(HttpHeaders.REFERER);
        String deviceType = detectDeviceType(userAgent);

        return new ResolutionContext(clientIp, userAgent, referer, null, deviceType, 50);
    }

    /**
     * Extract real client IP — respects proxy headers (CF-Connecting-IP, X-Real-IP, X-Forwarded-For).
     */
    private String extractClientIp(ServerHttpRequest request) {
        // Cloudflare
        String ip = request.getHeaders().getFirst("CF-Connecting-IP");
        if (ip != null && !ip.isBlank()) return ip.trim();

        // NGINX
        ip = request.getHeaders().getFirst("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip.trim();

        // Standard proxy header
        ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();

        // Direct connection
        var remoteAddr = request.getRemoteAddress();
        return remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : "unknown";
    }

    private String detectDeviceType(String userAgent) {
        if (userAgent == null) return "UNKNOWN";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "MOBILE";
        if (ua.contains("tablet") || ua.contains("ipad")) return "TABLET";
        return "DESKTOP";
    }
}
