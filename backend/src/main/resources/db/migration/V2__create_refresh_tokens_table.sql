CREATE TABLE refresh_tokens (
    id              BINARY(16)      NOT NULL,
    user_id         BINARY(16)      NOT NULL,
    token_hash      VARCHAR(255)    NOT NULL,
    expires_at      TIMESTAMP       NOT NULL,
    revoked_at      TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Cleanup job (Sprint 10) queries "expired/revoked tokens for this user"
-- and "all tokens past their expiry regardless of user" — this composite
-- index serves both access patterns.
CREATE INDEX idx_refresh_tokens_user_expiry ON refresh_tokens (user_id, expires_at);
