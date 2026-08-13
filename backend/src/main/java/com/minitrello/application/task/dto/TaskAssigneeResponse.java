package com.minitrello.application.task.dto;

import java.util.UUID;

public record TaskAssigneeResponse(UUID userId, String fullName, String email) {
}
