package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.task.TaskTag;
import com.minitrello.domain.task.TaskTagRepository;
import com.minitrello.infrastructure.persistence.jpa.TaskTagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TaskTagRepositoryImpl implements TaskTagRepository {

    private final TaskTagJpaRepository jpaRepository;

    @Override
    public TaskTag save(TaskTag taskTag) {
        return jpaRepository.save(taskTag);
    }

    @Override
    public List<TaskTag> findAllByTaskId(UUID taskId) {
        return jpaRepository.findAllByTask_Id(taskId);
    }

    @Override
    @Transactional
    public void deleteByTaskIdAndTagId(UUID taskId, UUID tagId) {
        jpaRepository.deleteByTask_IdAndTag_Id(taskId, tagId);
    }

    @Override
    public boolean existsByTaskIdAndTagId(UUID taskId, UUID tagId) {
        return jpaRepository.existsByTask_IdAndTag_Id(taskId, tagId);
    }
}
