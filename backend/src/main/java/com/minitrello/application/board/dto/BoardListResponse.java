package com.minitrello.application.board.dto;

import java.util.UUID;

public record BoardListResponse(UUID id, UUID boardId, String name, String position) {
}
