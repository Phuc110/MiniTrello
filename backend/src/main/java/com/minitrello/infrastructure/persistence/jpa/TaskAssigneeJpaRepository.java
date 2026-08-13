package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.task.TaskAssignee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskAssigneeJpaRepository extends JpaRepository<TaskAssignee, UUID> {
    List<TaskAssignee> findAllByTask_Id(UUID taskId);
    void deleteByTask_IdAndUser_Id(UUID taskId, UUID userId);
    boolean existsByTask_IdAndUser_Id(UUID taskId, UUID userId);
}
