package com.minitrello.application.task.dto;

import com.minitrello.domain.task.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTaskRequest(

        @NotBlank(message = "Task title is required")
        @Size(max = 255, message = "Task title must not exceed 255 characters")
        String title,

        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description,

        @NotNull(message = "Priority is required")
        Priority priority,

        LocalDate dueDate
) {
}
