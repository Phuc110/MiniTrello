package com.minitrello.domain.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findById(UUID id);

    List<Task> findAllByBoardListIdOrderByPosition(UUID boardListId);

    Optional<Task> findFirstByBoardListIdAndPositionLessThanOrderByPositionDesc(UUID boardListId, String position);

    Optional<Task> findFirstByBoardListIdAndPositionGreaterThanOrderByPositionAsc(UUID boardListId, String position);

    List<Task> searchByTitleOrDescription(UUID boardListId, String query);

    /** Used by the deadline notification scheduler — returns all non-deleted tasks that have a due date set. */
    List<Task> findAllWithDueDate();

    void softDeleteByProjectId(UUID projectId);

    void softDeleteByWorkspaceId(UUID workspaceId);
}

