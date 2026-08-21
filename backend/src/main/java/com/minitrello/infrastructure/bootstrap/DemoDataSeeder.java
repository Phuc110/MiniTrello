package com.minitrello.infrastructure.bootstrap;

import com.minitrello.domain.board.Board;
import com.minitrello.domain.board.BoardList;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.domain.board.BoardRepository;
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

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String PM_EMAIL = "pm@test.com";
    private static final String MEMBER_EMAIL = "member@test.com";
    private static final String DEMO_PASSWORD = "DemoPass1";

    private static final String WORKSPACE_SLUG = "demo-workspace";
    private static final String LAB_SLUG = "innovation-lab";

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
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

        seedDemoWorkspace(pm, member);
        seedInnovationLab(pm, member);

        log.info("Demo data seeded — log in as '{}' or '{}' with password '{}'.",
                PM_EMAIL, MEMBER_EMAIL, DEMO_PASSWORD);
    }

    private void seedDemoWorkspace(User pm, User member) {
        Workspace workspace = Workspace.builder()
                .name("Demo Workspace")
                .slug(WORKSPACE_SLUG)
                .ownerId(pm.getId())
                .build();
        workspace = workspaceRepository.save(workspace);

        workspaceMemberRepository.save(WorkspaceMember.builder().workspace(workspace).user(pm).build());
        workspaceMemberRepository.save(WorkspaceMember.builder().workspace(workspace).user(member).build());

        Tag bug = tagRepository.save(Tag.builder().workspaceId(workspace.getId()).name("Bug").color("#e5484d").build());
        Tag feature = tagRepository.save(Tag.builder().workspaceId(workspace.getId()).name("Feature").color("#30a46c").build());
        Tag design = tagRepository.save(Tag.builder().workspaceId(workspace.getId()).name("Design").color("#8e4ec6").build());
        Tag research = tagRepository.save(Tag.builder().workspaceId(workspace.getId()).name("Research").color("#f76b15").build());

        seedProductRoadmapBoard(workspace, pm, member, bug, feature, design);
        seedWebsiteRedesignBoard(workspace, pm, member, design, research, feature);
    }

    private void seedProductRoadmapBoard(Workspace workspace, User pm, User member,
                                         Tag bug, Tag feature, Tag design) {
        Board board = boardRepository.save(Board.builder()
                .workspaceId(workspace.getId())
                .name("Product Roadmap")
                .build());

        BoardList backlog = seedList(board, "Backlog");
        BoardList todo = seedList(board, "To Do");
        BoardList inProgress = seedList(board, "In Progress");
        BoardList review = seedList(board, "In Review");
        BoardList blocked = seedList(board, "Blocked");
        BoardList done = seedList(board, "Done");

        String pos = null;
        pos = seedTask(backlog, "Explore AI-powered summaries",
                "Spike: auto-summarize long task threads with an LLM.", Priority.LOW, null, null, feature, pos);
        seedTask(backlog, "Multi-workspace dashboard",
                "Unified view of tasks across every workspace.", Priority.MEDIUM, null, null, feature, pos);

        pos = null;
        pos = seedTask(todo, "Design landing page mockups",
                "High-fidelity mockups for the new landing page.", Priority.HIGH,
                LocalDate.now().plusDays(7), member, design, pos);
        pos = seedTask(todo, "Write API integration tests",
                "Cover auth, workspace membership, and drag-and-drop ordering.", Priority.MEDIUM,
                LocalDate.now().plusDays(10), member, feature, pos);
        seedTask(todo, "Research competitor pricing",
                "Summarize pricing pages of the top three competitors.", Priority.LOW,
                null, null, feature, pos);

        pos = null;
        pos = seedTask(inProgress, "Set up CI/CD pipeline",
                "GitHub Actions build + publish to GHCR on merge to main.", Priority.URGENT,
                LocalDate.now().plusDays(2), pm, feature, pos);
        seedTask(inProgress, "Fix task drag-and-drop glitch",
                "Cards occasionally jump lists on fast drags in Safari.", Priority.HIGH,
                LocalDate.now().plusDays(3), pm, bug, pos);

        pos = null;
        seedTask(review, "Review rate-limiting strategy",
                "Redis token bucket vs in-memory bucket for API throttling.", Priority.MEDIUM,
                LocalDate.now().plusDays(1), pm, feature, pos);

        pos = null;
        pos = seedTask(blocked, "Awaiting design tokens from brand team",
                "Blocked until the final color palette is approved.", Priority.MEDIUM,
                LocalDate.now().plusDays(5), member, design, pos);
        seedTask(blocked, "Legal review of data-retention policy",
                "Compliance sign-off needed before enabling auto-purge.", Priority.HIGH,
                LocalDate.now().minusDays(1), pm, bug, pos);

        pos = null;
        pos = seedTask(done, "Scaffold Spring Boot backend",
                "Clean Architecture skeleton with JWT auth and Flyway.", Priority.MEDIUM,
                null, pm, feature, pos);
        seedTask(done, "Draft project brief",
                "Initial scope, roles, and timeline.", Priority.LOW,
                null, member, design, pos);
    }

    private void seedWebsiteRedesignBoard(Workspace workspace, User pm, User member,
                                          Tag design, Tag research, Tag feature) {
        Board board = boardRepository.save(Board.builder()
                .workspaceId(workspace.getId())
                .name("Website Redesign")
                .build());

        BoardList discovery = seedList(board, "Discovery");
        BoardList wireframes = seedList(board, "Wireframes");
        BoardList uiDesign = seedList(board, "UI Design");
        BoardList development = seedList(board, "Development");
        BoardList done = seedList(board, "Done");

        String pos = null;
        pos = seedTask(discovery, "Stakeholder interviews",
                "Talk to sales, support, and marketing about site pain points.", Priority.HIGH,
                LocalDate.now().minusDays(2), member, research, pos);
        seedTask(discovery, "Analytics baseline report",
                "Capture current traffic, bounce rates, and funnels.", Priority.MEDIUM,
                LocalDate.now().plusDays(4), member, research, pos);

        pos = null;
        pos = seedTask(wireframes, "Home page wireframe v2",
                "Hero, social proof, pricing teaser, CTA sections.", Priority.HIGH,
                LocalDate.now().plusDays(3), member, design, pos);
        seedTask(wireframes, "Pricing page wireframe",
                "Three-tier layout with comparison table.", Priority.MEDIUM,
                LocalDate.now().plusDays(6), member, design, pos);

        pos = null;
        pos = seedTask(uiDesign, "Design system tokens",
                "Colors, typography, spacing, and component states.", Priority.URGENT,
                LocalDate.now().plusDays(1), pm, design, pos);
        pos = seedTask(uiDesign, "Dark mode palette",
                "Accessible dark theme variants for all core screens.", Priority.MEDIUM,
                LocalDate.now().plusDays(8), member, design, pos);
        seedTask(uiDesign, "404 & error page illustrations",
                "Friendly illustrations for error states.", Priority.LOW,
                null, null, design, pos);

        pos = null;
        pos = seedTask(development, "Migrate marketing site to React",
                "Rebuild pages on the shared component library.", Priority.HIGH,
                LocalDate.now().plusDays(12), pm, feature, pos);
        seedTask(development, "SEO meta + Open Graph tags",
                "Server-rendered metadata for every route.", Priority.MEDIUM,
                LocalDate.now().plusDays(9), member, feature, pos);

        pos = null;
        seedTask(done, "Heuristic evaluation of old site",
                "Usability audit against Nielsen's heuristics.", Priority.LOW,
                null, member, research, pos);
    }

    private void seedInnovationLab(User pm, User member) {
        Workspace workspace = Workspace.builder()
                .name("Innovation Lab")
                .slug(LAB_SLUG)
                .ownerId(pm.getId())
                .build();
        workspace = workspaceRepository.save(workspace);

        workspaceMemberRepository.save(WorkspaceMember.builder().workspace(workspace).user(pm).build());
        workspaceMemberRepository.save(WorkspaceMember.builder().workspace(workspace).user(member).build());

        Tag ios = tagRepository.save(Tag.builder().workspaceId(workspace.getId()).name("iOS").color("#0ea5e9").build());
        Tag android = tagRepository.save(Tag.builder().workspaceId(workspace.getId()).name("Android").color("#22c55e").build());
        Tag api = tagRepository.save(Tag.builder().workspaceId(workspace.getId()).name("API").color("#eab308").build());

        Board board = boardRepository.save(Board.builder()
                .workspaceId(workspace.getId())
                .name("Mobile App Launch")
                .build());

        BoardList backlog = seedList(board, "Backlog");
        BoardList design = seedList(board, "Design");
        BoardList todo = seedList(board, "To Do");
        BoardList inProgress = seedList(board, "In Progress");
        BoardList testing = seedList(board, "Testing");
        BoardList done = seedList(board, "Done");

        String pos = null;
        pos = seedTask(backlog, "Push notification service",
                "Evaluate FCM topics vs per-device tokens.", Priority.MEDIUM, null, null, api, pos);
        seedTask(backlog, "Offline mode cache",
                "Local-first sync for task mutations.", Priority.LOW, null, null, android, pos);

        pos = null;
        pos = seedTask(design, "Onboarding flow screens",
                "Three-slide intro with sign-in / sign-up entry points.", Priority.HIGH,
                LocalDate.now().plusDays(5), member, ios, pos);
        seedTask(design, "App icon & splash variants",
                "Light/dark adaptive icons for both platforms.", Priority.MEDIUM,
                LocalDate.now().plusDays(7), member, ios, pos);

        pos = null;
        pos = seedTask(todo, "Biometric login",
                "Face ID / fingerprint unlock using platform APIs.", Priority.HIGH,
                LocalDate.now().plusDays(14), null, android, pos);
        seedTask(todo, "Deep linking schema",
                "minitrello:// routes for boards, lists, and tasks.", Priority.MEDIUM,
                LocalDate.now().plusDays(11), pm, api, pos);

        pos = null;
        pos = seedTask(inProgress, "Kanban gesture polish",
                "Long-press drag with haptic feedback.", Priority.URGENT,
                LocalDate.now().plusDays(2), pm, ios, pos);
        seedTask(inProgress, "Sync engine prototype",
                "Optimistic writes with conflict resolution.", Priority.HIGH,
                LocalDate.now().plusDays(4), pm, api, pos);

        pos = null;
        seedTask(testing, "Beta TestFlight round 1",
                "Internal smoke test of auth and board flows.", Priority.HIGH,
                LocalDate.now().plusDays(6), member, ios, pos);

        pos = null;
        seedTask(done, "Project kickoff & roadmap",
                "Scope, milestones, and launch criteria agreed.", Priority.MEDIUM,
                null, pm, api, pos);
    }

    private BoardList seedList(Board board, String name) {
        String lastPosition = boardListRepository.findAllByBoardIdOrderByPosition(board.getId()).stream()
                .reduce((first, second) -> second)
                .map(BoardList::getPosition)
                .orElse(null);
        return boardListRepository.save(BoardList.builder()
                .boardId(board.getId())
                .name(name)
                .position(lastPosition == null ? PositionGenerator.initial() : PositionGenerator.after(lastPosition))
                .build());
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
