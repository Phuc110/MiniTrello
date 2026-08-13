CREATE TABLE board_lists (
    id              BINARY(16)      NOT NULL,
    board_id        BINARY(16)      NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    position        VARCHAR(255)    NOT NULL,
    deleted_at      TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_board_lists_board
        FOREIGN KEY (board_id) REFERENCES boards (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Board rendering always fetches all lists for a board, in position order.
CREATE INDEX idx_board_lists_board_position ON board_lists (board_id, position);
