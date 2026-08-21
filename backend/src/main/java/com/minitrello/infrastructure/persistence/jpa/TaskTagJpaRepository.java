package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.task.TaskTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskTagJpaRepository extends JpaRepository<TaskTag, UUID> {
    List<TaskTag> findAllByTask_Id(UUID taskId);
    void deleteByTask_IdAndTag_Id(UUID taskId, UUID tagId);
    void deleteByTag_Id(UUID tagId);
    boolean existsByTask_IdAndTag_Id(UUID taskId, UUID tagId);
}
