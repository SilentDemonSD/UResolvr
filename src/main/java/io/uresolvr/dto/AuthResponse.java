package io.uresolvr.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    String tokenType
) {
    public static AuthResponse bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new AuthResponse(accessToken, refreshToken, expiresInSeconds, "Bearer");
    }
}
