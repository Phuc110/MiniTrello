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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tags")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "Create a tag in a project's tag vocabulary")
    @PostMapping("/api/projects/{projectId}/tags")
    public ResponseEntity<ApiResponse<TagResponse>> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTagRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        TagResponse response = tagService.createTag(projectId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tag created successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "List all tags available in a project")
    @GetMapping("/api/projects/{projectId}/tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        List<TagResponse> response = tagService.listForProject(projectId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }
}
