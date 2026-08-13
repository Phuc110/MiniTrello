package com.minitrello.presentation.controller;

import com.minitrello.application.project.ProjectService;
import com.minitrello.application.project.dto.CreateProjectRequest;
import com.minitrello.application.project.dto.InviteProjectMemberRequest;
import com.minitrello.application.project.dto.ProjectMemberResponse;
import com.minitrello.application.project.dto.ProjectResponse;
import com.minitrello.application.project.dto.UpdateMemberRoleRequest;
import com.minitrello.application.project.dto.UpdateProjectRequest;
import com.minitrello.application.shared.ApiResponse;
import com.minitrello.application.shared.PageResponse;
import com.minitrello.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projects")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Create a project in a workspace — caller must be a workspace member and becomes the project OWNER")
    @PostMapping("/api/workspaces/{workspaceId}/projects")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        ProjectResponse response = projectService.createProject(workspaceId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Project created successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Search/list projects in a workspace that the caller is a member of, paginated")
    @GetMapping("/api/workspaces/{workspaceId}/projects")
    public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>> search(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        PageResponse<ProjectResponse> response =
                projectService.searchProjects(workspaceId, principal.getId(), name, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Get a project by id — caller must be a project member")
    @GetMapping("/api/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        ProjectResponse response = projectService.getProject(projectId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Update a project's name/description — requires OWNER or MANAGER role")
    @PutMapping("/api/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        ProjectResponse response = projectService.updateProject(projectId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Project updated successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Soft-delete a project — requires OWNER role")
    @DeleteMapping("/api/projects/{projectId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        projectService.deleteProject(projectId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Project deleted successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Invite an existing user to the project by email — requires OWNER or MANAGER role")
    @PostMapping("/api/projects/{projectId}/members")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> inviteMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody InviteProjectMemberRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        ProjectMemberResponse response = projectService.inviteMember(projectId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Member invited successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "List all members of a project — caller must be a project member")
    @GetMapping("/api/projects/{projectId}/members")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> listMembers(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        List<ProjectMemberResponse> response = projectService.listMembers(projectId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Remove a member from a project — requires OWNER or MANAGER role")
    @DeleteMapping("/api/projects/{projectId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        projectService.removeMember(projectId, principal.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Change a member's role — requires OWNER role")
    @PatchMapping("/api/projects/{projectId}/members/{userId}/role")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> updateMemberRole(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        ProjectMemberResponse response =
                projectService.updateMemberRole(projectId, principal.getId(), userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Member role updated successfully", httpRequest.getRequestURI()));
    }
}
