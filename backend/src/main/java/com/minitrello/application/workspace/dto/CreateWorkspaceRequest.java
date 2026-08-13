package com.minitrello.application.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank(message = "Workspace name is required")
        @Size(max = 150, message = "Workspace name must not exceed 150 characters")
        String name
) {
}
