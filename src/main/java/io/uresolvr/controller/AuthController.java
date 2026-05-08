package io.uresolvr.controller;

import io.uresolvr.dto.AuthResponse;
import io.uresolvr.dto.LoginRequest;
import io.uresolvr.dto.SetupRequest;
import io.uresolvr.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/setup")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> setup(@Valid @RequestBody SetupRequest request) {
        return authService.setup(request);
    }

    @GetMapping("/setup/status")
    public Mono<Map<String, Boolean>> setupStatus() {
        return authService.isSetupRequired()
            .map(required -> Map.of("setupRequired", required));
    }
}
