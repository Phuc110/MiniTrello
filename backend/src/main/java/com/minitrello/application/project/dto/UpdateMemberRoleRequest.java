package com.minitrello.application.project.dto;

import com.minitrello.domain.project.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required")
        ProjectRole role
) {
}
