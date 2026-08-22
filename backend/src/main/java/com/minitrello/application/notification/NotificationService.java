package com.minitrello.application.notification;

import com.minitrello.domain.board.BoardList;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.domain.notification.Notification;
import com.minitrello.domain.notification.NotificationRepository;
import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import com.minitrello.domain.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final BoardListRepository boardListRepository;

    /** Returns ALL notifications for the caller (newest-first). */
    @Transactional(readOnly = true)
    public List<NotificationResponse> listAll(UUID callerId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(callerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Returns only UNREAD notifications for the caller. */
    @Transactional(readOnly = true)
    public List<NotificationResponse> listUnread(UUID callerId) {
        return notificationRepository.findAllByUserIdAndReadFalseOrderByCreatedAtDesc(callerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Count of unread notifications — used by the frontend badge. */
    @Transactional(readOnly = true)
    public long countUnread(UUID callerId) {
        return notificationRepository.countByUserIdAndReadFalse(callerId);
    }

    /**
     * Marks one notification as read.
     * Ownership check: if the notification does not belong to callerId we throw 403.
     */
    @Transactional
    public NotificationResponse markRead(UUID notificationId, UUID callerId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!notification.getUserId().equals(callerId)) {
            throw new ForbiddenOperationException("You do not own this notification");
        }
        notification.markRead();
        notification = notificationRepository.save(notification);
        return toResponse(notification);
    }

    /** Marks ALL notifications for the caller as read. */
    @Transactional
    public void markAllRead(UUID callerId) {
        notificationRepository.markAllReadByUserId(callerId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTaskId(),
                resolveBoardId(n),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }

    /**
     * task -> boardList -> board walk for deep-linking. Strict (respects
     * @SQLRestriction) on purpose: if the task/list/board was soft-deleted,
     * navigating there would dead-end, so we hand back null and let the
     * client skip the navigation step.
     */
    private UUID resolveBoardId(Notification n) {
        if (n.getTaskId() == null) {
            return null;
        }
        return taskRepository.findById(n.getTaskId())
                .flatMap(task -> boardListRepository.findById(task.getBoardListId()))
                .map(BoardList::getBoardId)
                .orElse(null);
    }
}
