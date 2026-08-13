package com.minitrello.infrastructure.bootstrap;

import com.minitrello.domain.board.Board;
import com.minitrello.domain.board.BoardList;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.domain.board.BoardRepository;
import com.minitrello.domain.project.Project;
import com.minitrello.domain.project.ProjectMember;
import com.minitrello.domain.project.ProjectMemberRepository;
import com.minitrello.domain.project.ProjectRepository;
import com.minitrello.domain.project.ProjectRole;
import com.minitrello.domain.shared.PositionGenerator;
import com.minitrello.domain.task.Priority;
import com.minitrello.domain.task.Tag;
import com.minitrello.domain.task.TagRepository;
import com.minitrello.domain.task.Task;
import com.minitrello.domain.task.TaskAssignee;
import com.minitrello.domain.task.TaskAssigneeRepository;
import com.minitrello.domain.task.TaskRepository;
import com.minitrello.domain.task.TaskTag;
import com.minitrello.domain.task.TaskTagRepository;
import com.minitrello.domain.user.SystemRole;
import com.minitrello.domain.user.User;
import com.minitrello.domain.user.UserRepository;
import com.minitrello.domain.workspace.Workspace;
import com.minitrello.domain.workspace.WorkspaceMember;
import com.minitrello.domain.workspace.WorkspaceMemberRepository;
import com.minitrello.domain.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Bootstraps a small, self-contained demo dataset so a fresh clone can be
 * explored immediately after `docker compose up` without manually creating
 * users, a workspace, a project, and a populated board through the UI.
 *
 * Design notes:
 * - Opt-in only: gated on `app.demo-data.enabled` (env {@code DEMO_DATA_ENABLED}),
 *   which defaults to {@code false} in application.yml — production deploys
 *   never seed unless explicitly enabled, and DEPLOYMENT.md says to keep it
 *   off there.
 * - Idempotent: keyed on the demo workspace slug; a second boot simply
 *   skips. Safe to re-run across restarts.
 * - Deliberately uses the domain-layer repository ports (same ones the
 *   application services use) rather than raw JPA, so the seed path never
 *   bypasses the same persistence layer the app relies on.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String PM_EMAIL = "pm@test.com";
    private static final String MEMBER_EMAIL = "member@test.com";
    private static final String DEMO_PASSWORD = "DemoPass1";

    private static final String WORKSPACE_SLUG = "demo-workspace";

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardRepository boardRepository;
    private final BoardListRepository boardListRepository;
    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final TaskTagRepository taskTagRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (workspaceRepository.existsBySlug(WORKSPACE_SLUG)) {
            log.info("Demo data already present — skipping seed.");
            return;
        }

        User pm = userRepository.findByEmail(PM_EMAIL).orElseGet(() -> createUser(PM_EMAIL, "Demo PM"));
        User member = userRepository.findByEmail(MEMBER_EMAIL).orElseGet(() -> createUser(MEMBER_EMAIL, "Demo Member"));

        Workspace workspace = Workspace.builder()
                .name("Demo Workspace")
                .slug(WORKSPACE_SLUG)
                .ownerId(pm.getId())
                .build();
        workspace = workspaceRepository.save(workspace);

        workspaceMemberRepository.save(WorkspaceMember.builder().workspace(workspace).user(pm).build());
        workspaceMemberRepository.save(WorkspaceMember.builder().workspace(workspace).user(member).build());

        Project project = Project.builder()
                .workspaceId(workspace.getId())
                .name("Website Launch")
                .description("Q3 go-live for the public marketing site and customer portal.")
                .build();
        project = projectRepository.save(project);

        projectMemberRepository.save(ProjectMember.builder()
                .project(project).user(pm).role(ProjectRole.OWNER).build());
        projectMemberRepository.save(ProjectMember.builder()
                .project(project).user(member).role(ProjectRole.CONTRIBUTOR).build());

        Board board = Board.builder()
                .projectId(project.getId())
                .name("Product Roadmap")
                .build();
        board = boardRepository.save(board);

        BoardList todo = boardListRepository.save(BoardList.builder()
                .boardId(board.getId()).name("To Do").position(PositionGenerator.initial()).build());
        BoardList inProgress = boardListRepository.save(BoardList.builder()
                .boardId(board.getId()).name("In Progress").position(PositionGenerator.after(todo.getPosition())).build());
        BoardList done = boardListRepository.save(BoardList.builder()
                .boardId(board.getId()).name("Done").position(PositionGenerator.after(inProgress.getPosition())).build());

        Tag bug = tagRepository.save(Tag.builder().projectId(project.getId()).name("Bug").color("#e5484d").build());
        Tag feature = tagRepository.save(Tag.builder().projectId(project.getId()).name("Feature").color("#30a46c").build());
        Tag design = tagRepository.save(Tag.builder().projectId(project.getId()).name("Design").color("#8e4ec6").build());

        String todoPos = PositionGenerator.initial();
        todoPos = seedTask(todo, "Design landing page mockups",
                "High-fidelity mockups for the new landing page.", Priority.HIGH,
                LocalDate.now().plusDays(7), member, design, todoPos);
        todoPos = seedTask(todo, "Write API integration tests",
                "Cover auth, project membership, and drag-and-drop ordering.", Priority.MEDIUM,
                null, member, feature, todoPos);
        seedTask(todo, "Research competitor pricing",
                "Summarize pricing pages of the top three competitors.", Priority.LOW,
                null, null, feature, todoPos);

        String inProgressPos = PositionGenerator.initial();
        inProgressPos = seedTask(inProgress, "Set up CI/CD pipeline",
                "GitHub Actions build + publish to GHCR on merge to main.", Priority.URGENT,
                LocalDate.now().plusDays(2), pm, feature, inProgressPos);
        seedTask(inProgress, "Fix task drag-and-drop glitch",
                "Cards occasionally jump lists on fast drags in Safari.", Priority.HIGH,
                LocalDate.now().plusDays(3), pm, bug, inProgressPos);

        String donePos = PositionGenerator.initial();
        donePos = seedTask(done, "Scaffold Spring Boot backend",
                "Clean Architecture skeleton with JWT auth and Flyway.", Priority.MEDIUM,
                null, pm, feature, donePos);
        seedTask(done, "Draft project brief",
                "Initial scope, roles, and timeline.", Priority.LOW,
                null, member, design, donePos);

        log.info("Demo data seeded — log in as '{}' or '{}' with password '{}'.",
                PM_EMAIL, MEMBER_EMAIL, DEMO_PASSWORD);
    }

    private User createUser(String email, String fullName) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .fullName(fullName)
                .systemRole(SystemRole.MEMBER)
                .build());
    }

    private String seedTask(BoardList list, String title, String description,
                            Priority priority, LocalDate dueDate, User assignee, Tag tag, String prevPosition) {
        String position = prevPosition == null
                ? PositionGenerator.initial()
                : PositionGenerator.after(prevPosition);

        Task task = taskRepository.save(Task.builder()
                .boardListId(list.getId())
                .title(title)
                .description(description)
                .priority(priority)
                .position(position)
                .dueDate(dueDate)
                .build());

        if (assignee != null) {
            taskAssigneeRepository.save(TaskAssignee.builder().task(task).user(assignee).build());
        }
        if (tag != null) {
            taskTagRepository.save(TaskTag.builder().task(task).tag(tag).build());
        }
        return position;
    }
}
