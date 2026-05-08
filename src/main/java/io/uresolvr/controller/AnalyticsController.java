package io.uresolvr.controller;

import io.uresolvr.repository.ResolutionEventRepository;
import io.uresolvr.repository.RouteRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final ResolutionEventRepository eventRepository;
    private final RouteRepository routeRepository;

    public AnalyticsController(ResolutionEventRepository eventRepository,
                                RouteRepository routeRepository) {
        this.eventRepository = eventRepository;
        this.routeRepository = routeRepository;
    }

    @GetMapping("/summary")
    public Mono<Map<String, Object>> getSummary(@AuthenticationPrincipal UUID tenantId,
                                                 @RequestParam(defaultValue = "24") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        return Mono.zip(
            routeRepository.countByTenantId(tenantId),
            eventRepository.countByTenantIdAndResolvedAtAfter(tenantId, since)
        ).map(tuple -> Map.of(
            "totalRoutes", (Object) tuple.getT1(),
            "resolutionsLast" + hours + "h", tuple.getT2(),
            "periodHours", hours
        ));
    }

    @GetMapping("/outcomes")
    public Mono<Map<String, Long>> getOutcomeBreakdown(@AuthenticationPrincipal UUID tenantId,
                                                        @RequestParam(defaultValue = "24") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        return eventRepository.countByOutcome(tenantId, since)
            .collectMap(
                ResolutionEventRepository.OutcomeCount::getOutcome,
                ResolutionEventRepository.OutcomeCount::getCnt
            );
    }
}
