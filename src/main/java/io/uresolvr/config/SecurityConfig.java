package io.uresolvr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Public resolution endpoint — the hot path
                .pathMatchers(HttpMethod.GET, "/r/**").permitAll()
                // Static assets & web UI
                .pathMatchers("/", "/index.html", "/setup", "/login",
                    "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                // Actuator health
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                // API docs
                .pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                // Auth endpoints
                .pathMatchers("/api/v1/auth/**").permitAll()
                // Setup wizard (first-run only)
                .pathMatchers("/api/v1/setup/**").permitAll()
                // Everything else requires authentication
                .anyExchange().authenticated()
            )
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .headers(headers -> headers
                .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable)
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "img-src 'self' data:; connect-src 'self'"
                ))
            )
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
