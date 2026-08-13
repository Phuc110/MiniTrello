package com.minitrello.application.notification;

import com.minitrello.domain.notification.Notification;
import com.minitrello.domain.notification.NotificationRepository;
import com.minitrello.domain.notification.NotificationType;
import com.minitrello.domain.task.Task;
import com.minitrello.domain.task.TaskAssignee;
import com.minitrello.domain.task.TaskAssigneeRepository;
import com.minitrello.domain.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Runs every 5 minutes and scans all tasks with a due date.
 *
 * Notification lifecycle:
 *   DEADLINE_SOON  — dueDate is tomorrow or the day after (within 48 hours)
 *   DUE_TODAY      — dueDate == today (Vietnam local date)
 *   OVERDUE        — dueDate < today
 *
 * Idempotency: the unique constraint on (user_id, task_id, type, due_date_snapshot)
 * prevents duplicate rows — we explicitly check with existsByKey before inserting.
 * Duplicate inserts would fail at the DB layer too, but we skip the DB round-trip.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineNotificationScheduler {

    /** Vietnam timezone — matches the target users for this application. */
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final NotificationRepository notificationRepository;

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    @Transactional
    public void checkDeadlines() {
        LocalDate today = LocalDate.now(VN_ZONE);
        List<Task> tasks = taskRepository.findAllWithDueDate();

        int created = 0;
        int skipped = 0;

        for (Task task : tasks) {
            LocalDate dueDate = task.getDueDate();
            NotificationType type = classifyDueDate(dueDate, today);
            if (type == null) {
                // More than 2 days away — no notification yet
                continue;
            }

            List<TaskAssignee> assignees = taskAssigneeRepository.findAllByTaskId(task.getId());
            if (assignees.isEmpty()) {
                continue;
            }

            String title = buildTitle(type);
            String message = buildMessage(task.getTitle(), type, dueDate, today);

            for (TaskAssignee assignee : assignees) {
                boolean exists = notificationRepository.existsByUserIdAndTaskIdAndTypeAndDueDateSnapshot(
                        assignee.getUser().getId(), task.getId(), type, dueDate);
                if (exists) {
                    skipped++;
                    continue;
                }
                notificationRepository.save(Notification.builder()
                        .userId(assignee.getUser().getId())
                        .taskId(task.getId())
                        .type(type)
                        .title(title)
                        .message(message)
                        .dueDateSnapshot(dueDate)
                        .build());
                created++;
            }
        }

        if (created > 0 || skipped > 0) {
            log.info("[DeadlineScheduler] created={} skipped_duplicates={} tasks_checked={}",
                    created, skipped, tasks.size());
        }
    }

    /**
     * Maps a task's due date to the appropriate notification type.
     * Returns null when no notification should be generated yet.
     */
    private NotificationType classifyDueDate(LocalDate dueDate, LocalDate today) {
        if (dueDate.isBefore(today)) {
            return NotificationType.OVERDUE;
        }
        if (dueDate.isEqual(today)) {
            return NotificationType.DUE_TODAY;
        }
        long daysUntil = today.until(dueDate).getDays();
        if (daysUntil <= 2) {
            // within 48 hours (calendar-day based)
            return NotificationType.DEADLINE_SOON;
        }
        return null;
    }

    private String buildTitle(NotificationType type) {
        return switch (type) {
            case OVERDUE       -> "Task overdue";
            case DUE_TODAY     -> "Due today";
            case DEADLINE_SOON -> "Deadline approaching";
        };
    }

    private String buildMessage(String taskTitle, NotificationType type, LocalDate dueDate, LocalDate today) {
        return switch (type) {
            case OVERDUE -> {
                long daysAgo = dueDate.until(today).getDays();
                yield daysAgo == 1
                        ? "\"" + taskTitle + "\" was due yesterday."
                        : "\"" + taskTitle + "\" was due " + daysAgo + " days ago.";
            }
            case DUE_TODAY -> "\"" + taskTitle + "\" is due today.";
            case DEADLINE_SOON -> {
                long daysUntil = today.until(dueDate).getDays();
                yield daysUntil == 1
                        ? "\"" + taskTitle + "\" is due tomorrow."
                        : "\"" + taskTitle + "\" is due in " + daysUntil + " days.";
            }
        };
    }
}
