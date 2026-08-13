package com.minitrello.application.board.dto;

import java.time.Instant;
import java.util.UUID;

public record BoardResponse(UUID id, UUID projectId, String name, Instant createdAt) {
}
