package com.minitrello.application.auth.dto;

/**
 * Returned on login/register/refresh. The refresh token itself is
 * deliberately NOT included here — it's set as an httpOnly cookie by
 * AuthController directly, never exposed to JS-readable response bodies
 * (see Phase 2 authentication flow decisions).
 */
public record AuthResponse(
        String accessToken,
        long expiresInSeconds,
        UserResponse user
) {
}
