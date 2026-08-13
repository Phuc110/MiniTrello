package com.minitrello.domain.notification;

import com.minitrello.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A notification belonging to one user about one task's deadline.
 *
 * The unique constraint (user_id, task_id, type, due_date_snapshot) in
 * V11 migration guarantees idempotency — the scheduler can fire as many
 * times as it likes and will never create a duplicate.
 *
 * Note: does NOT extend SoftDeletableEntity intentionally — notifications
 * are never soft-deleted (they are removed via FK cascade when the task is
 * hard-deleted, or via the user account cascade).
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "task_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /**
     * The task's due date at the time the notification was generated.
     * Used together with (user_id, task_id, type) as the idempotency key.
     * If the task's due date is changed, a new notification may be created
     * for the new date while the old one is preserved.
     */
    @Column(name = "due_date_snapshot", nullable = false)
    private LocalDate dueDateSnapshot;

    @Column(name = "read_at")
    private Instant readAt;

    public void markRead() {
        this.read = true;
        this.readAt = Instant.now();
    }
}
