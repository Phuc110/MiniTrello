package com.minitrello.presentation.controller;

import com.minitrello.application.shared.ApiResponse;
import com.minitrello.application.task.TagService;
import com.minitrello.application.task.dto.CreateTagRequest;
import com.minitrello.application.task.dto.TagResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "Create a tag in a workspace's tag vocabulary")
    @PostMapping("/api/workspaces/{workspaceId}/tags")
    public ResponseEntity<ApiResponse<TagResponse>> create(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateTagRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        TagResponse response = tagService.createTag(workspaceId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tag created successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "List all tags available in a workspace")
    @GetMapping("/api/workspaces/{workspaceId}/tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> list(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        List<TagResponse> response = tagService.listForWorkspace(workspaceId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Delete a tag and detach it from all tasks")
    @DeleteMapping("/api/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID tagId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        tagService.deleteTag(tagId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Tag deleted successfully", httpRequest.getRequestURI()));
    }
}
