package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.user.RefreshToken;
import com.minitrello.domain.user.RefreshTokenRepository;
import com.minitrello.infrastructure.persistence.jpa.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return jpaRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash);
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        jpaRepository.revokeAllForUser(userId);
    }

    @Override
    @Transactional
    public void deleteExpiredBefore(Instant cutoff) {
        jpaRepository.deleteExpiredBefore(cutoff);
    }
}
