package com.minitrello.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates short-lived JWT ACCESS tokens only.
 *
 * Deliberately does NOT handle refresh tokens — those are opaque random
 * strings stored hashed in the database (see RefreshTokenService), not
 * JWTs. That split matters: an access token is stateless and self-
 * validating (fast, no DB hit per request), while a refresh token must be
 * revocable server-side at any time (e.g. on logout, or on detected
 * replay) — which a stateless JWT alone cannot do without a blocklist.
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMinutes;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-minutes}") long accessTokenExpirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
    }

    public String generateAccessToken(UUID userId, String email, String systemRole) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(accessTokenExpirationMinutes));

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", systemRole)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
