package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.task.Task;
import com.minitrello.domain.task.TaskRepository;
import com.minitrello.infrastructure.persistence.jpa.TaskJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskJpaRepository jpaRepository;

    @Override
    public Task save(Task task) {
        return jpaRepository.save(task);
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Task> findAllByBoardListIdOrderByPosition(UUID boardListId) {
        return jpaRepository.findAllByBoardListIdOrderByPositionAsc(boardListId);
    }

    @Override
    public Optional<Task> findFirstByBoardListIdAndPositionLessThanOrderByPositionDesc(UUID boardListId, String position) {
        return jpaRepository.findFirstByBoardListIdAndPositionLessThanOrderByPositionDesc(boardListId, position);
    }

    @Override
    public Optional<Task> findFirstByBoardListIdAndPositionGreaterThanOrderByPositionAsc(UUID boardListId, String position) {
        return jpaRepository.findFirstByBoardListIdAndPositionGreaterThanOrderByPositionAsc(boardListId, position);
    }

    @Override
    public List<Task> searchByTitleOrDescription(UUID boardListId, String query) {
        return jpaRepository.searchByTitleOrDescription(boardListId, query);
    }

    @Override
    public List<Task> findAllWithDueDate() {
        return jpaRepository.findAllWithDueDate();
    }

    @Override
    public void softDeleteByProjectId(UUID projectId) {
        jpaRepository.softDeleteByProjectId(projectId);
    }

    @Override
    public void softDeleteByWorkspaceId(UUID workspaceId) {
        jpaRepository.softDeleteByWorkspaceId(workspaceId);
    }
}

