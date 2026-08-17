package com.minitrello.integration.repository;

import com.minitrello.config.JpaAuditingConfig;
import com.minitrello.domain.project.Project;
import com.minitrello.domain.project.ProjectMember;
import com.minitrello.domain.project.ProjectRole;
import com.minitrello.domain.user.SystemRole;
import com.minitrello.domain.user.User;
import com.minitrello.domain.workspace.Workspace;
import com.minitrello.infrastructure.persistence.jpa.ProjectJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer test — real MySQL via Testcontainers, no HTTP layer,
 * no security filters. This exercises the single most important
 * authorization-adjacent query in the whole schema directly:
 * ProjectJpaRepository.searchForUser JOINs through ProjectMember so a
 * caller can never see a project via this query path without an actual
 * membership row (see the Phase 6 "no cross-tenant leakage" comment on
 * the query itself). A regression here would be a silent, serious
 * security bug — worth its own focused test independent of the full
 * HTTP-level integration tests.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class ProjectJpaRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("mini_trello_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mysql.getJdbcUrl() + "?allowPublicKeyRetrieval=true&useSSL=false");
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private TestEntityManager entityManager;

    @org.springframework.beans.factory.annotation.Autowired
    private ProjectJpaRepository projectJpaRepository;

    @Test
    void searchForUser_onlyReturnsProjectsTheCallerIsAMemberOf() {
        User memberUser = persistUser("member@example.com");
        User outsiderUser = persistUser("outsider@example.com");
        Workspace workspace = persistWorkspace(memberUser.getId());

        Project visibleProject = persistProject(workspace.getId(), "Visible Project");
        Project hiddenProject = persistProject(workspace.getId(), "Hidden Project");
        persistMembership(visibleProject, memberUser, ProjectRole.OWNER);
        // Note: memberUser is deliberately NOT added to hiddenProject.
        persistMembership(hiddenProject, outsiderUser, ProjectRole.OWNER);

        entityManager.flush();
        entityManager.clear();

        Page<Project> results = projectJpaRepository.searchForUser(
                workspace.getId(), memberUser.getId(), null, PageRequest.of(0, 10));

        assertThat(results.getContent()).extracting(Project::getName).containsExactly("Visible Project");
    }

    @Test
    void searchForUser_appliesCaseInsensitiveNameFilter() {
        User user = persistUser("filter-test@example.com");
        Workspace workspace = persistWorkspace(user.getId());
        Project matching = persistProject(workspace.getId(), "Website Redesign");
        Project nonMatching = persistProject(workspace.getId(), "Mobile App");
        persistMembership(matching, user, ProjectRole.OWNER);
        persistMembership(nonMatching, user, ProjectRole.OWNER);

        entityManager.flush();
        entityManager.clear();

        Page<Project> results = projectJpaRepository.searchForUser(
                workspace.getId(), user.getId(), "redesign", PageRequest.of(0, 10));

        assertThat(results.getContent()).extracting(Project::getName).containsExactly("Website Redesign");
    }

    @Test
    void searchForUser_excludesSoftDeletedProjects() {
        User user = persistUser("softdelete-test@example.com");
        Workspace workspace = persistWorkspace(user.getId());
        Project active = persistProject(workspace.getId(), "Active Project");
        Project deleted = persistProject(workspace.getId(), "Deleted Project");
        deleted.softDelete();
        entityManager.persistAndFlush(deleted);
        persistMembership(active, user, ProjectRole.OWNER);
        persistMembership(deleted, user, ProjectRole.OWNER);

        entityManager.flush();
        entityManager.clear();

        Page<Project> results = projectJpaRepository.searchForUser(
                workspace.getId(), user.getId(), null, PageRequest.of(0, 10));

        assertThat(results.getContent()).extracting(Project::getName).containsExactly("Active Project");
    }

    private User persistUser(String email) {
        User user = User.builder()
                .email(email)
                .passwordHash("hashed")
                .fullName("Test User")
                .systemRole(SystemRole.MEMBER)
                .build();
        return entityManager.persistAndFlush(user);
    }

    private Workspace persistWorkspace(java.util.UUID ownerId) {
        Workspace workspace = Workspace.builder()
                .name("Test Workspace")
                .slug("test-workspace-" + java.util.UUID.randomUUID())
                .ownerId(ownerId)
                .build();
        return entityManager.persistAndFlush(workspace);
    }

    private Project persistProject(java.util.UUID workspaceId, String name) {
        Project project = Project.builder()
                .workspaceId(workspaceId)
                .name(name)
                .build();
        return entityManager.persistAndFlush(project);
    }

    private void persistMembership(Project project, User user, ProjectRole role) {
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(role)
                .build();
        entityManager.persistAndFlush(member);
    }
}
