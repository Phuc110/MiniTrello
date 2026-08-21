package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.task.TaskAssignee;
import com.minitrello.domain.task.TaskAssigneeRepository;
import com.minitrello.infrastructure.persistence.jpa.TaskAssigneeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TaskAssigneeRepositoryImpl implements TaskAssigneeRepository {

    private final TaskAssigneeJpaRepository jpaRepository;

    @Override
    public TaskAssignee save(TaskAssignee assignee) {
        return jpaRepository.save(assignee);
    }

    @Override
    public List<TaskAssignee> findAllByTaskId(UUID taskId) {
        return jpaRepository.findAllByTask_Id(taskId);
    }

    @Override
    public List<TaskAssignee> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUser_Id(userId);
    }

    @Override
    @Transactional
    public void deleteByTaskIdAndUserId(UUID taskId, UUID userId) {
        jpaRepository.deleteByTask_IdAndUser_Id(taskId, userId);
    }

    @Override
    public boolean existsByTaskIdAndUserId(UUID taskId, UUID userId) {
        return jpaRepository.existsByTask_IdAndUser_Id(taskId, userId);
    }
}
