package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.notification.Notification;
import com.minitrello.domain.notification.NotificationRepository;
import com.minitrello.domain.notification.NotificationType;
import com.minitrello.infrastructure.persistence.jpa.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(Notification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId) {
        return jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Notification> findAllByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId) {
        return jpaRepository.findAllByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public long countByUserIdAndReadFalse(UUID userId) {
        return jpaRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public boolean existsByUserIdAndTaskIdAndTypeAndDueDateSnapshot(
            UUID userId, UUID taskId, NotificationType type, LocalDate dueDateSnapshot) {
        return jpaRepository.existsByUserIdAndTaskIdAndTypeAndDueDateSnapshot(userId, taskId, type, dueDateSnapshot);
    }

    @Override
    @Transactional
    public void markAllReadByUserId(UUID userId) {
        jpaRepository.markAllReadByUserId(userId);
    }
}
