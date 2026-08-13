CREATE TABLE workspaces (
    id              BINARY(16)      NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    slug            VARCHAR(150)    NOT NULL,
    owner_id        BINARY(16)      NOT NULL,
    deleted_at      TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_workspaces_slug UNIQUE (slug),
    CONSTRAINT fk_workspaces_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
