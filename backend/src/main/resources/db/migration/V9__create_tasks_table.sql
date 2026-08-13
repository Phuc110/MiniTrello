CREATE TABLE tasks (
    id              BINARY(16)      NOT NULL,
    board_list_id   BINARY(16)      NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    description     TEXT            NULL,
    priority        VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    position        VARCHAR(255)    NOT NULL,
    due_date        DATE            NULL,
    deleted_at      TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT fk_tasks_board_list
        FOREIGN KEY (board_list_id) REFERENCES board_lists (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Board column rendering: all tasks in a list, in position order.
CREATE INDEX idx_tasks_list_position ON tasks (board_list_id, position);

-- Dashboard / "overdue tasks" queries (Sprint 7).
CREATE INDEX idx_tasks_due_date ON tasks (due_date);

-- Backs the MATCH...AGAINST full-text search used by TaskJpaRepository.
CREATE FULLTEXT INDEX ft_tasks_title_description ON tasks (title, description);
