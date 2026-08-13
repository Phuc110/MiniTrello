package com.minitrello.application.board.dto;

import java.util.UUID;

/**
 * Describes the desired new neighbors of a moved list. Both are nullable:
 * omit prevListId to move to the front, omit nextListId to move to the
 * back. The server computes the new lexicographic position from these —
 * the client never sends a position value directly, so it can't corrupt
 * ordering by racing another client's drag.
 */
public record MoveBoardListRequest(UUID prevListId, UUID nextListId) {
}
