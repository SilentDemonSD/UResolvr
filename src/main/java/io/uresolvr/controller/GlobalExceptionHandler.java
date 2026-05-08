package io.uresolvr.controller;

import io.uresolvr.dto.ProblemDetail;
import io.uresolvr.service.ResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * Global exception handler — returns RFC 7807 Problem Detail responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleValidation(WebExchangeBindException ex) {
        String errors = ex.getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return Mono.just(ResponseEntity.badRequest()
            .body(ProblemDetail.badRequest(errors)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleBadRequest(IllegalArgumentException ex) {
        return Mono.just(ResponseEntity.badRequest()
            .body(ProblemDetail.badRequest(ex.getMessage())));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleConflict(IllegalStateException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ProblemDetail.conflict(ex.getMessage())));
    }

    @ExceptionHandler(SecurityException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleSecurity(SecurityException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ProblemDetail.of(401, "Unauthorized", ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ProblemDetail.internal("An unexpected error occurred")));
    }
}
