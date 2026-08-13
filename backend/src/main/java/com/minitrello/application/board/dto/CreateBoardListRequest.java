package com.minitrello.application.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardListRequest(
        @NotBlank(message = "List name is required")
        @Size(max = 150, message = "List name must not exceed 150 characters")
        String name
) {
}
