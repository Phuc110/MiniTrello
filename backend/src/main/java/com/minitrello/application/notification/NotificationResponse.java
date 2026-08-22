package com.minitrello.application.notification;

import com.minitrello.domain.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID taskId,
        /**
         * Resolved server-side via task -> boardList -> board so clients can
         * deep-link straight to the board containing the task. Null when the
         * task/list/board no longer exists (soft-deleted) — the client then
         * only marks the notification read without navigating.
         */
        UUID boardId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
