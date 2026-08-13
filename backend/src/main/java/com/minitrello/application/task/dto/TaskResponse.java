package com.minitrello.application.task.dto;

import com.minitrello.domain.task.Priority;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID boardListId,
        String title,
        String description,
        Priority priority,
        String position,
        LocalDate dueDate,
        List<TaskAssigneeResponse> assignees,
        List<TagResponse> tags,
        Instant createdAt
) {
}
