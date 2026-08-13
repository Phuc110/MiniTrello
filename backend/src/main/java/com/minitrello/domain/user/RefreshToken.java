package com.minitrello.domain.user;

import com.minitrello.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Represents an issued refresh token. We never store the raw token value —
 * only a SHA-256 hash of it — so a database leak alone can't be used to
 * forge sessions (mirrors how we'd never store a plaintext password).
 *
 * Rotation model: every successful /auth/refresh call revokes the current
 * row (sets revokedAt) and issues a brand new one. If a revoked token is
 * ever presented again, that's a signal of theft/replay and — in
 * AuthService — triggers revocation of ALL of that user's active tokens.
 *
 * No SoftDeletableEntity here: revocation IS the "deletion" mechanism, and
 * expired/revoked rows are purged by a scheduled job (Phase 10), not
 * soft-deleted.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }
}
