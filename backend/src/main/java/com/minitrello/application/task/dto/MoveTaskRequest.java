package com.minitrello.application.task.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Drag-and-drop move — targetBoardListId lets a task move to a DIFFERENT
 * column, not just be reordered within its current one (a card being
 * dragged from "To Do" to "In Progress" is the same operation as
 * reordering within a column, just with a different destination list).
 */
public record MoveTaskRequest(
        @NotNull(message = "Target list is required")
        UUID targetBoardListId,
        UUID prevTaskId,
        UUID nextTaskId
) {
}
