package io.uresolvr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * RFC 7807 Problem Detail for HTTP APIs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    Instant timestamp
) {
    public static ProblemDetail of(int status, String title, String detail) {
        return new ProblemDetail(
            "about:blank", title, status, detail, null, Instant.now()
        );
    }

    public static ProblemDetail notFound(String detail) {
        return of(404, "Not Found", detail);
    }

    public static ProblemDetail forbidden(String detail) {
        return of(403, "Forbidden", detail);
    }

    public static ProblemDetail badRequest(String detail) {
        return of(400, "Bad Request", detail);
    }

    public static ProblemDetail tooManyRequests(String detail) {
        return of(429, "Too Many Requests", detail);
    }

    public static ProblemDetail conflict(String detail) {
        return of(409, "Conflict", detail);
    }

    public static ProblemDetail internal(String detail) {
        return of(500, "Internal Server Error", detail);
    }
}
