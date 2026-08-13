package com.minitrello.application.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(

        @NotBlank(message = "Tag name is required")
        @Size(max = 50, message = "Tag name must not exceed 50 characters")
        String name,

        @NotBlank(message = "Color is required")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex code like #FF5733")
        String color
) {
}
