package io.uresolvr.service;

import io.uresolvr.repository.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Background tasks — expires overdue routes, cleans up analytics data, etc.
 */
@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    private final RouteRepository routeRepository;

    public MaintenanceService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    /**
     * Expire routes past their expiration timestamp.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 60_000)
    public void expireOverdueRoutes() {
        routeRepository.expireOverdueRoutes()
            .subscribe(count -> {
                if (count > 0) {
                    log.info("Expired {} overdue routes", count);
                }
            });
    }
}
