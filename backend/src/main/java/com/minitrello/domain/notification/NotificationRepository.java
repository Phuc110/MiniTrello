package com.minitrello.domain.notification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    /** All notifications for a user, newest-first. */
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Unread notifications for a user, newest-first. */
    List<Notification> findAllByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    /** Count of unread notifications for a user. */
    long countByUserIdAndReadFalse(UUID userId);

    /**
     * Idempotency check — returns true if a notification with the same
     * (userId, taskId, type, dueDateSnapshot) already exists so the
     * scheduler does not create duplicates.
     */
    boolean existsByUserIdAndTaskIdAndTypeAndDueDateSnapshot(
            UUID userId, UUID taskId, NotificationType type, LocalDate dueDateSnapshot);

    /** Mark all notifications for a user as read. */
    void markAllReadByUserId(UUID userId);
}
