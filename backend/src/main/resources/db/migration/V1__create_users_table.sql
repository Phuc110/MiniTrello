CREATE TABLE users (
    id              BINARY(16)      NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(150)    NOT NULL,
    system_role     VARCHAR(20)     NOT NULL DEFAULT 'MEMBER',
    deleted_at      TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_system_role CHECK (system_role IN ('ADMIN', 'MEMBER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Login is the single hottest query on this table; UNIQUE already
-- creates an index, this comment documents why it must never be dropped.
-- (uq_users_email doubles as the login lookup index)
