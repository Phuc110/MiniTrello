CREATE TABLE boards (
    id              BINARY(16)      NOT NULL,
    project_id      BINARY(16)      NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    deleted_at      TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_boards_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_boards_project ON boards (project_id);
