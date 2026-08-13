CREATE TABLE projects (
    id              BINARY(16)      NOT NULL,
    workspace_id    BINARY(16)      NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    description     TEXT            NULL,
    deleted_at      TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_projects_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Every "list active projects in workspace X" query filters on both
-- columns together — most-selective column (workspace_id) first.
CREATE INDEX idx_projects_workspace_deleted ON projects (workspace_id, deleted_at);
