package com.minitrello.unit.project;

import com.minitrello.application.project.ProjectAuthorizationService;
import com.minitrello.domain.project.Project;
import com.minitrello.domain.project.ProjectMember;
import com.minitrello.domain.project.ProjectMemberRepository;
import com.minitrello.domain.project.ProjectRole;
import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the full project permission matrix in isolation from Spring
 * and the database — this is exactly the kind of business-rule test the
 * domain/application split in Phase 2 exists to make cheap and fast.
 */
@ExtendWith(MockitoExtension.class)
class ProjectAuthorizationServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectAuthorizationService authorizationService;

    private ProjectMember memberWithRole(ProjectRole role) {
        return ProjectMember.builder()
                .project(Project.builder().build())
                .role(role)
                .build();
    }

    @ParameterizedTest
    @EnumSource(value = ProjectRole.class, names = {"OWNER", "MANAGER"})
    void requireCanUpdateProject_allowsOwnerAndManager(ProjectRole role) {
        assertThatCode(() -> authorizationService.requireCanUpdateProject(memberWithRole(role)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = ProjectRole.class, names = {"CONTRIBUTOR", "VIEWER"})
    void requireCanUpdateProject_rejectsContributorAndViewer(ProjectRole role) {
        assertThatThrownBy(() -> authorizationService.requireCanUpdateProject(memberWithRole(role)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void requireCanDeleteProject_onlyAllowsOwner() {
        assertThatCode(() -> authorizationService.requireCanDeleteProject(memberWithRole(ProjectRole.OWNER)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> authorizationService.requireCanDeleteProject(memberWithRole(ProjectRole.MANAGER)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void requireCanManageMember_managerCanInviteContributor() {
        ProjectMember manager = memberWithRole(ProjectRole.MANAGER);
        assertThatCode(() -> authorizationService.requireCanManageMember(manager, ProjectRole.CONTRIBUTOR))
                .doesNotThrowAnyException();
    }

    @Test
    void requireCanManageMember_managerCannotPromoteToOwner() {
        ProjectMember manager = memberWithRole(ProjectRole.MANAGER);
        assertThatThrownBy(() -> authorizationService.requireCanManageMember(manager, ProjectRole.OWNER))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Only an OWNER");
    }

    @Test
    void requireCanManageMember_managerCannotAssignManagerRole() {
        ProjectMember manager = memberWithRole(ProjectRole.MANAGER);
        assertThatThrownBy(() -> authorizationService.requireCanManageMember(manager, ProjectRole.MANAGER))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void requireCanManageMember_ownerCanAssignAnyRole() {
        ProjectMember owner = memberWithRole(ProjectRole.OWNER);
        assertThatCode(() -> authorizationService.requireCanManageMember(owner, ProjectRole.MANAGER))
                .doesNotThrowAnyException();
        assertThatCode(() -> authorizationService.requireCanManageMember(owner, ProjectRole.OWNER))
                .doesNotThrowAnyException();
    }

    @Test
    void requireCanManageMember_contributorCannotManageAnyone() {
        ProjectMember contributor = memberWithRole(ProjectRole.CONTRIBUTOR);
        assertThatThrownBy(() -> authorizationService.requireCanManageMember(contributor, ProjectRole.VIEWER))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void requireCanChangeRole_onlyAllowsOwner() {
        assertThatCode(() -> authorizationService.requireCanChangeRole(memberWithRole(ProjectRole.OWNER)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> authorizationService.requireCanChangeRole(memberWithRole(ProjectRole.MANAGER)))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
