package com.minitrello.application.project;

import com.minitrello.application.project.dto.CreateProjectRequest;
import com.minitrello.application.project.dto.InviteProjectMemberRequest;
import com.minitrello.application.project.dto.ProjectMemberResponse;
import com.minitrello.application.project.dto.ProjectResponse;
import com.minitrello.application.project.dto.UpdateMemberRoleRequest;
import com.minitrello.application.project.dto.UpdateProjectRequest;
import com.minitrello.application.shared.PageResponse;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.domain.board.BoardRepository;
import com.minitrello.domain.project.Project;
import com.minitrello.domain.project.ProjectMember;
import com.minitrello.domain.project.ProjectMemberRepository;
import com.minitrello.domain.project.ProjectRepository;
import com.minitrello.domain.project.ProjectRole;
import com.minitrello.domain.shared.exception.BusinessRuleViolationException;
import com.minitrello.domain.shared.exception.DuplicateResourceException;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import com.minitrello.domain.task.TaskRepository;
import com.minitrello.domain.user.User;
import com.minitrello.domain.user.UserRepository;
import com.minitrello.domain.workspace.Workspace;
import com.minitrello.domain.workspace.WorkspaceMember;
import com.minitrello.domain.workspace.WorkspaceMemberRepository;
import com.minitrello.domain.workspace.WorkspaceRepository;
import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ProjectMapper projectMapper;
    private final BoardRepository boardRepository;
    private final BoardListRepository boardListRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public ProjectResponse createProject(UUID workspaceId, UUID callerId, CreateProjectRequest request) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, callerId)) {
            throw new ForbiddenOperationException("You must be a workspace member to create a project in it");
        }

        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", callerId));

        Project project = Project.builder()
                .workspaceId(workspaceId)
                .name(request.name().trim())
                .description(request.description())
                .build();
        project = projectRepository.save(project);

        // Creator is always the initial OWNER — ownership can be
        // transferred later via updateMemberRole, but every project must
        // have at least one OWNER at creation time.
        ProjectMember ownerMembership = ProjectMember.builder()
                .project(project)
                .user(caller)
                .role(ProjectRole.OWNER)
                .build();
        projectMemberRepository.save(ownerMembership);

        return projectMapper.toResponse(project, ProjectRole.OWNER);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, UUID callerId) {
        Project project = requireProject(projectId);
        ProjectMember membership = authorizationService.requireMembership(projectId, callerId);
        return projectMapper.toResponse(project, membership.getRole());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> searchProjects(
            UUID workspaceId, UUID callerId, String nameFilter, Pageable pageable) {
        Page<Project> page = projectRepository.searchForUser(workspaceId, callerId, nameFilter, pageable);
        Page<ProjectResponse> mapped = page.map(project -> {
            // Role lookup per row is acceptable at this scale (paginated,
            // max ~50 rows/page); if this ever shows up in profiling, batch
            // via a single IN-query keyed by project id instead.
            ProjectMember membership = authorizationService.requireMembership(project.getId(), callerId);
            return projectMapper.toResponse(project, membership.getRole());
        });
        return PageResponse.from(mapped);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UUID callerId, UpdateProjectRequest request) {
        Project project = requireProject(projectId);
        ProjectMember membership = authorizationService.requireMembership(projectId, callerId);
        authorizationService.requireCanUpdateProject(membership);

        project.setName(request.name().trim());
        project.setDescription(request.description());
        project = projectRepository.save(project);

        return projectMapper.toResponse(project, membership.getRole());
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID callerId) {
        Project project = requireProject(projectId);
        ProjectMember membership = authorizationService.requireMembership(projectId, callerId);
        authorizationService.requireCanDeleteProject(membership);

        // Cascade soft-delete: tasks → board_lists → boards (deepest first
        // to avoid FK issues if hard-delete purge runs between steps).
        taskRepository.softDeleteByProjectId(projectId);
        boardListRepository.softDeleteByProjectId(projectId);
        boardRepository.softDeleteByProjectId(projectId);
        projectMemberRepository.deleteByProjectId(projectId);

        project.softDelete();
        projectRepository.save(project);
    }

    @Transactional
    public ProjectMemberResponse inviteMember(UUID projectId, UUID callerId, InviteProjectMemberRequest request) {
        requireProject(projectId);
        ProjectMember actor = authorizationService.requireMembership(projectId, callerId);
        authorizationService.requireCanManageMember(actor, request.role());

        User invitee = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email " + request.email() + " — they must register before being invited"));

        if (projectMemberRepository.findByProjectIdAndUserId(projectId, invitee.getId()).isPresent()) {
            throw new DuplicateResourceException("This user is already a member of the project");
        }

        ProjectMember newMember = ProjectMember.builder()
                .project(actor.getProject())
                .user(invitee)
                .role(request.role())
                .build();
        newMember = projectMemberRepository.save(newMember);

        // Being a project member requires workspace access context so workspace queries succeed
        UUID workspaceId = actor.getProject().getWorkspaceId();
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, invitee.getId())) {
            Workspace workspace = workspaceRepository.findById(workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
            workspaceMemberRepository.save(WorkspaceMember.builder()
                    .workspace(workspace)
                    .user(invitee)
                    .build());
        }

        return projectMapper.toMemberResponse(newMember);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(UUID projectId, UUID callerId) {
        authorizationService.requireMembership(projectId, callerId);
        return projectMemberRepository.findAllByProjectId(projectId).stream()
                .map(projectMapper::toMemberResponse)
                .toList();
    }

    @Transactional
    public void removeMember(UUID projectId, UUID callerId, UUID targetUserId) {
        ProjectMember actor = authorizationService.requireMembership(projectId, callerId);
        ProjectMember target = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project membership", targetUserId));

        authorizationService.requireCanManageMember(actor, target.getRole());
        guardAgainstRemovingLastOwner(projectId, target);

        projectMemberRepository.delete(target);
    }

    @Transactional
    public ProjectMemberResponse updateMemberRole(
            UUID projectId, UUID callerId, UUID targetUserId, UpdateMemberRoleRequest request) {
        ProjectMember actor = authorizationService.requireMembership(projectId, callerId);
        authorizationService.requireCanChangeRole(actor);

        ProjectMember target = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project membership", targetUserId));

        if (target.getRole() == ProjectRole.OWNER && request.role() != ProjectRole.OWNER) {
            guardAgainstRemovingLastOwner(projectId, target);
        }

        target.setRole(request.role());
        target = projectMemberRepository.save(target);

        return projectMapper.toMemberResponse(target);
    }

    private void guardAgainstRemovingLastOwner(UUID projectId, ProjectMember target) {
        if (target.getRole() == ProjectRole.OWNER
                && projectMemberRepository.countByProjectIdAndRole(projectId, ProjectRole.OWNER) <= 1) {
            throw new BusinessRuleViolationException(
                    "Cannot remove or demote the last remaining OWNER of a project — transfer ownership first");
        }
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }
}
