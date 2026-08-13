CREATE TABLE project_members (
    id              BINARY(16)      NOT NULL,
    project_id      BINARY(16)      NOT NULL,
    user_id         BINARY(16)      NOT NULL,
    project_role    VARCHAR(20)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_project_members_project_user UNIQUE (project_id, user_id),
    CONSTRAINT chk_project_members_role
        CHECK (project_role IN ('OWNER', 'MANAGER', 'CONTRIBUTOR', 'VIEWER')),
    CONSTRAINT fk_project_members_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_project_members_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- THE hottest authorization-check query in the whole system — every
-- protected project/board/task action looks up (project_id, user_id).
CREATE INDEX idx_project_members_user ON project_members (user_id);
