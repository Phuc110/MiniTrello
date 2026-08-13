package com.minitrello.application.project.dto;

import com.minitrello.domain.project.ProjectRole;

import java.util.UUID;

public record ProjectMemberResponse(
        UUID userId,
        String email,
        String fullName,
        ProjectRole role
) {
}
