package com.minitrello.application.workspace;

import com.minitrello.application.workspace.dto.CreateWorkspaceRequest;
import com.minitrello.application.workspace.dto.WorkspaceResponse;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.domain.board.BoardRepository;
import com.minitrello.domain.project.ProjectMemberRepository;
import com.minitrello.domain.project.ProjectRepository;
import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import com.minitrello.domain.task.TaskRepository;
import com.minitrello.domain.user.SystemRole;
import com.minitrello.domain.user.User;
import com.minitrello.domain.user.UserRepository;
import com.minitrello.domain.workspace.Workspace;
import com.minitrello.domain.workspace.WorkspaceMember;
import com.minitrello.domain.workspace.WorkspaceMemberRepository;
import com.minitrello.domain.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final WorkspaceMapper workspaceMapper;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardRepository boardRepository;
    private final BoardListRepository boardListRepository;
    private final TaskRepository taskRepository;


    @Transactional
    public WorkspaceResponse createWorkspace(UUID ownerId, CreateWorkspaceRequest request) {
        User owner = requireUser(ownerId);

        String slug = generateUniqueSlug(request.name());

        Workspace workspace = Workspace.builder()
                .name(request.name().trim())
                .slug(slug)
                .ownerId(ownerId)
                .build();
        workspace = workspaceRepository.save(workspace);

        // The owner is always also a WorkspaceMember — ownership implies
        // membership, but membership does not imply ownership.
        workspaceMemberRepository.save(WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .build());

        return workspaceMapper.toResponse(workspace, true);
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(UUID workspaceId, UUID callerId) {
        Workspace workspace = requireWorkspace(workspaceId);
        requireMembership(workspaceId, callerId);
        User caller = requireUser(callerId);
        return workspaceMapper.toResponse(workspace, canDelete(workspace, caller));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMyWorkspaces(UUID callerId) {
        User caller = requireUser(callerId);
        return workspaceMemberRepository.findAllByUserId(callerId).stream()
                .map(member -> workspaceMapper.toResponse(member.getWorkspace(), canDelete(member.getWorkspace(), caller)))
                .toList();
    }

    private Workspace requireWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void requireMembership(UUID workspaceId, UUID userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new ForbiddenOperationException("You are not a member of this workspace");
        }
    }

    /**
     * Server-side source of truth for whether the caller may delete the
     * workspace: only the owner or a system ADMIN may. Kept in one place so
     * the delete endpoint and the response flag can never disagree.
     */
    private boolean canDelete(Workspace workspace, User caller) {
        return workspace.getOwnerId().equals(caller.getId())
                || caller.getSystemRole() == SystemRole.ADMIN;
    }

    @Transactional
    public void deleteWorkspace(UUID workspaceId, UUID callerId) {
        Workspace workspace = requireWorkspace(workspaceId);

        User caller = requireUser(callerId);

        if (!canDelete(workspace, caller)) {
            throw new ForbiddenOperationException("Only the workspace owner can delete it");
        }

        // Cascade soft-delete from the deepest layer up so a concurrent
        // purge job never sees orphaned children with deletedAt=null.
        taskRepository.softDeleteByWorkspaceId(workspaceId);
        boardListRepository.softDeleteByWorkspaceId(workspaceId);
        boardRepository.softDeleteByWorkspaceId(workspaceId);
        projectMemberRepository.deleteByWorkspaceId(workspaceId);
        projectRepository.softDeleteByWorkspaceId(workspaceId);
        workspaceMemberRepository.deleteByWorkspaceId(workspaceId);

        workspace.softDelete();
        workspaceRepository.save(workspace);
    }

    /**
     * Slugs are derived from the name and de-duplicated with a numeric
     * suffix on collision (acme-corp, acme-corp-2, ...) rather than a
     * random suffix, so URLs stay predictable and human-friendly.
     */
    private String generateUniqueSlug(String name) {
        String base = slugify(name);
        String candidate = base;
        int suffix = 2;
        while (workspaceRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
        String slug = NON_SLUG_CHARS.matcher(normalized).replaceAll("-");
        return slug.replaceAll("^-+|-+$", "");
    }
}
