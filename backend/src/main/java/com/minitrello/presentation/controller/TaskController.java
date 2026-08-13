package com.minitrello.presentation.controller;

import com.minitrello.application.shared.ApiResponse;
import com.minitrello.application.task.TaskService;
import com.minitrello.application.task.dto.CreateTaskRequest;
import com.minitrello.application.task.dto.MoveTaskRequest;
import com.minitrello.application.task.dto.TaskResponse;
import com.minitrello.application.task.dto.UpdateTaskRequest;
import com.minitrello.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tasks")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Create a task in a list — appended at the end by default")
    @PostMapping("/api/board-lists/{boardListId}/tasks")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @PathVariable UUID boardListId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        TaskResponse response = taskService.createTask(boardListId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Task created successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "List all tasks in a list, in display order")
    @GetMapping("/api/board-lists/{boardListId}/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> list(
            @PathVariable UUID boardListId,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        List<TaskResponse> response = (search == null || search.isBlank())
                ? taskService.listForBoardList(boardListId, principal.getId())
                : taskService.search(boardListId, principal.getId(), search);
        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Get a single task by id")
    @GetMapping("/api/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        TaskResponse response = taskService.getTask(taskId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Update a task's title, description, priority, or due date")
    @PutMapping("/api/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        TaskResponse response = taskService.updateTask(taskId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Task updated successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Soft-delete a task")
    @DeleteMapping("/api/tasks/{taskId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        taskService.deleteTask(taskId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Task deleted successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Drag-and-drop move — reposition within a list or move to a different list")
    @PatchMapping("/api/tasks/{taskId}/position")
    public ResponseEntity<ApiResponse<TaskResponse>> move(
            @PathVariable UUID taskId,
            @Valid @RequestBody MoveTaskRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        TaskResponse response = taskService.moveTask(taskId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Task moved successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Assign a user to a task")
    @PostMapping("/api/tasks/{taskId}/assignees/{userId}")
    public ResponseEntity<ApiResponse<Void>> assign(
            @PathVariable UUID taskId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        taskService.assignUser(taskId, principal.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "User assigned successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Remove a user's assignment from a task")
    @DeleteMapping("/api/tasks/{taskId}/assignees/{userId}")
    public ResponseEntity<ApiResponse<Void>> unassign(
            @PathVariable UUID taskId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        taskService.unassignUser(taskId, principal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User unassigned successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Apply a tag to a task")
    @PostMapping("/api/tasks/{taskId}/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> addTag(
            @PathVariable UUID taskId,
            @PathVariable UUID tagId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        taskService.addTag(taskId, principal.getId(), tagId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Tag applied successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Remove a tag from a task")
    @DeleteMapping("/api/tasks/{taskId}/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> removeTag(
            @PathVariable UUID taskId,
            @PathVariable UUID tagId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        taskService.removeTag(taskId, principal.getId(), tagId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tag removed successfully", httpRequest.getRequestURI()));
    }
}
