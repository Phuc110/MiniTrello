package com.minitrello.presentation.controller;

import com.minitrello.application.shared.ApiResponse;
import com.minitrello.application.workspace.WorkspaceService;
import com.minitrello.application.workspace.dto.CreateWorkspaceRequest;
import com.minitrello.application.workspace.dto.InviteMemberRequest;
import com.minitrello.application.workspace.dto.WorkspaceMemberResponse;
import com.minitrello.application.workspace.dto.WorkspaceResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

@Tag(name = "Workspaces")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "Create a new workspace — the caller becomes its owner")
    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        WorkspaceResponse response = workspaceService.createWorkspace(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Workspace created successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "List all workspaces the caller belongs to")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> listMine(
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        List<WorkspaceResponse> workspaces = workspaceService.listMyWorkspaces(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(workspaces, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Get a single workspace by id — caller must be a member")
    @GetMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getById(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        WorkspaceResponse response = workspaceService.getWorkspace(workspaceId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Delete a workspace and all its contents — caller must be the owner")
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        workspaceService.deleteWorkspace(workspaceId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List all members in a workspace")
    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<ApiResponse<List<WorkspaceMemberResponse>>> listMembers(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        List<WorkspaceMemberResponse> members = workspaceService.listMembers(workspaceId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(members, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Invite a user to the workspace by email")
    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> inviteMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        WorkspaceMemberResponse member = workspaceService.inviteMember(workspaceId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(member, "Member invited successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Remove a member from the workspace")
    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        workspaceService.removeMember(workspaceId, principal.getId(), userId);
        return ResponseEntity.noContent().build();
    }
}
