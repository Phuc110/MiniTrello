package com.minitrello.application.project;

import com.minitrello.domain.project.ProjectMember;
import com.minitrello.domain.project.ProjectMemberRepository;
import com.minitrello.domain.project.ProjectRole;
import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Single source of truth for "who can do what" on a project. Centralizing
 * this (rather than scattering role checks across ProjectService methods)
 * means the actual permission matrix is readable in one place and testable
 * in isolation — see ProjectAuthorizationServiceTest.
 *
 * Deliberately does NOT use ProjectRole.ordinal() for comparisons (see the
 * warning on the enum itself) — every check below is an explicit,
 * named set of allowed roles.
 */
@Component
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    private static final Set<ProjectRole> CAN_UPDATE_PROJECT = EnumSet.of(ProjectRole.OWNER, ProjectRole.MANAGER);
    private static final Set<ProjectRole> CAN_DELETE_PROJECT = EnumSet.of(ProjectRole.OWNER);
    private static final Set<ProjectRole> CAN_MANAGE_MEMBERS = EnumSet.of(ProjectRole.OWNER, ProjectRole.MANAGER);
    private static final Set<ProjectRole> CAN_CHANGE_ROLES = EnumSet.of(ProjectRole.OWNER);

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectMember requireMembership(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ForbiddenOperationException("You are not a member of this project"));
    }

    public void requireCanUpdateProject(ProjectMember member) {
        requireRole(member, CAN_UPDATE_PROJECT, "update this project");
    }

    public void requireCanDeleteProject(ProjectMember member) {
        requireRole(member, CAN_DELETE_PROJECT, "delete this project");
    }

    /**
     * A MANAGER may invite/remove CONTRIBUTOR and VIEWER members, but only
     * an OWNER may grant or revoke OWNER/MANAGER-level access — otherwise
     * a MANAGER could promote themselves (or an ally) to OWNER and lock
     * out the real owner.
     */
    public void requireCanManageMember(ProjectMember actor, ProjectRole targetRole) {
        requireRole(actor, CAN_MANAGE_MEMBERS, "manage members on this project");

        boolean targetIsPrivileged = targetRole == ProjectRole.OWNER || targetRole == ProjectRole.MANAGER;
        if (targetIsPrivileged && actor.getRole() != ProjectRole.OWNER) {
            throw new ForbiddenOperationException("Only an OWNER can assign OWNER or MANAGER roles");
        }
    }

    public void requireCanChangeRole(ProjectMember actor) {
        requireRole(actor, CAN_CHANGE_ROLES, "change member roles on this project");
    }

    private void requireRole(ProjectMember member, Set<ProjectRole> allowedRoles, String action) {
        if (!allowedRoles.contains(member.getRole())) {
            throw new ForbiddenOperationException(
                    "Your role (%s) does not permit you to %s".formatted(member.getRole(), action));
        }
    }
}
