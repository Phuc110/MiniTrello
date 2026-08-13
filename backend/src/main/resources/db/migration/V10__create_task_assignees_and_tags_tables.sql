CREATE TABLE task_assignees (
    id              BINARY(16)      NOT NULL,
    task_id         BINARY(16)      NOT NULL,
    user_id         BINARY(16)      NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_task_assignees_task_user UNIQUE (task_id, user_id),
    CONSTRAINT fk_task_assignees_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_task_assignees_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_task_assignees_user ON task_assignees (user_id);

CREATE TABLE tags (
    id              BINARY(16)      NOT NULL,
    project_id      BINARY(16)      NOT NULL,
    name            VARCHAR(50)     NOT NULL,
    color           VARCHAR(7)      NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_tags_project_name UNIQUE (project_id, name),
    CONSTRAINT fk_tags_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE task_tags (
    id              BINARY(16)      NOT NULL,
    task_id         BINARY(16)      NOT NULL,
    tag_id          BINARY(16)      NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_task_tags_task_tag UNIQUE (task_id, tag_id),
    CONSTRAINT fk_task_tags_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_task_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_task_tags_tag ON task_tags (tag_id);
