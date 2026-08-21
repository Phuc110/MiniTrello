package com.minitrello.application.board.dto;

import java.time.Instant;
import java.util.UUID;

public record BoardResponse(UUID id, UUID workspaceId, String name, Instant createdAt) {
}
