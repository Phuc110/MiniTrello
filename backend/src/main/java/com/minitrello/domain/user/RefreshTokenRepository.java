package com.minitrello.domain.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllForUser(UUID userId);

    void deleteExpiredBefore(Instant cutoff);
}
