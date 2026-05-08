package io.uresolvr.controller;

import io.uresolvr.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

/**
 * Serves the web UI — redirects to setup wizard if first run, otherwise to dashboard.
 */
@Controller
public class WebController {

    private final AuthService authService;

    public WebController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public Mono<String> index() {
        return authService.isSetupRequired()
            .map(required -> required ? "redirect:/setup" : "redirect:/dashboard");
    }

    @GetMapping("/setup")
    public Mono<String> setup() {
        return Mono.just("setup");
    }

    @GetMapping("/login")
    public Mono<String> login() {
        return Mono.just("login");
    }

    @GetMapping("/dashboard")
    public Mono<String> dashboard() {
        return Mono.just("dashboard");
    }
}
