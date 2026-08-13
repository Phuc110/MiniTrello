package com.minitrello.application.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardRequest(
        @NotBlank(message = "Board name is required")
        @Size(max = 150, message = "Board name must not exceed 150 characters")
        String name
) {
}
