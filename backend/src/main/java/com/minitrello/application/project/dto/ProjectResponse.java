package com.minitrello.application.project.dto;

import com.minitrello.domain.project.ProjectRole;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String description,
        /** The CALLER's role on this project — lets the frontend show/hide actions without a second request. */
        ProjectRole callerRole,
        Instant createdAt
) {
}
