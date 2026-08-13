package com.minitrello.application.auth.dto;

import com.minitrello.domain.user.SystemRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        SystemRole systemRole
) {
}
