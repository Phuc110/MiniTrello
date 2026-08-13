CREATE TABLE workspace_members (
    id              BINARY(16)      NOT NULL,
    workspace_id    BINARY(16)      NOT NULL,
    user_id         BINARY(16)      NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_workspace_members_workspace_user UNIQUE (workspace_id, user_id),
    CONSTRAINT fk_workspace_members_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_workspace_members_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- "list my workspaces" is a top-of-every-session query.
CREATE INDEX idx_workspace_members_user ON workspace_members (user_id);
