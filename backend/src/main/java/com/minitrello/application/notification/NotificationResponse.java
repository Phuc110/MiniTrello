package com.minitrello.application.notification;

import com.minitrello.domain.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID taskId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
