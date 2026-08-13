package com.minitrello.application.project.dto;

import com.minitrello.domain.project.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteProjectMemberRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotNull(message = "Role is required")
        ProjectRole role
) {
}
