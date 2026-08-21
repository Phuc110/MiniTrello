package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.task.TaskAssignee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskAssigneeJpaRepository extends JpaRepository<TaskAssignee, UUID> {
    List<TaskAssignee> findAllByTask_Id(UUID taskId);

    @Query("SELECT ta FROM TaskAssignee ta JOIN ta.task t WHERE ta.user.id = :userId AND t.deletedAt IS NULL")
    List<TaskAssignee> findAllByUser_Id(@Param("userId") UUID userId);

    void deleteByTask_IdAndUser_Id(UUID taskId, UUID userId);
    boolean existsByTask_IdAndUser_Id(UUID taskId, UUID userId);
}
