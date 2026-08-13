package com.minitrello.application.auth;

import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import com.minitrello.domain.user.RefreshToken;
import com.minitrello.domain.user.RefreshTokenRepository;
import com.minitrello.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Owns the full lifecycle of opaque refresh tokens: issuing, hashing,
 * validating, and rotating them. Kept separate from AuthService so the
 * "how do we handle refresh tokens" mechanism is independently testable
 * and reusable (e.g. if we ever add a "log out of all devices" endpoint,
 * it just calls revokeAllForUser here).
 *
 * Token format: 256 bits of SecureRandom, base64url-encoded — this is the
 * raw value returned to the client. Only its SHA-256 hash is persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    public String issue(User user) {
        String rawToken = generateRawToken();

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plus(Duration.ofDays(refreshTokenExpirationDays)))
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates the presented raw refresh token and rotates it: the old
     * token row is revoked and a brand new one is issued, bound to the
     * same user. Rotation on every use means a stolen-but-unused token
     * becomes worthless the moment the legitimate client refreshes again.
     *
     * If a token that is already revoked is presented, that's a strong
     * signal of theft or replay (the legitimate client already rotated
     * past it) — we respond by revoking ALL of that user's active
     * sessions, forcing a fresh login everywhere.
     */
    public RotationResult rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ForbiddenOperationException("Invalid refresh token"));

        if (existing.isRevoked()) {
            log.warn("Reuse of revoked refresh token detected for user {} — revoking all sessions", existing.getUser().getId());
            refreshTokenRepository.revokeAllForUser(existing.getUser().getId());
            throw new ForbiddenOperationException("Refresh token has already been used. All sessions have been revoked for your safety.");
        }

        if (existing.isExpired()) {
            throw new ForbiddenOperationException("Refresh token has expired. Please log in again.");
        }

        existing.revoke();
        refreshTokenRepository.save(existing);

        String newRawToken = issue(existing.getUser());
        return new RotationResult(existing.getUser(), newRawToken);
    }

    public void revokeAllForUser(java.util.UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every JVM; this can never happen in practice.
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public record RotationResult(User user, String rawRefreshToken) {
    }
}
