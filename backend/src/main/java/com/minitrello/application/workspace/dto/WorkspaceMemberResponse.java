package com.minitrello.application.workspace.dto;

import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID userId,
        String email,
        String fullName
) {
}
