package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.notification.Notification;
import com.minitrello.domain.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findAllByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    boolean existsByUserIdAndTaskIdAndTypeAndDueDateSnapshot(
            UUID userId, UUID taskId, NotificationType type, LocalDate dueDateSnapshot);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.read = false")
    void markAllReadByUserId(@Param("userId") UUID userId);
}
