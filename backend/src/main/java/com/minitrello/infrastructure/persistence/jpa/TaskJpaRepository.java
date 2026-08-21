package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.task.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskJpaRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByBoardListIdOrderByPositionAsc(UUID boardListId);

    Optional<Task> findFirstByBoardListIdAndPositionLessThanOrderByPositionDesc(UUID boardListId, String position);

    Optional<Task> findFirstByBoardListIdAndPositionGreaterThanOrderByPositionAsc(UUID boardListId, String position);

    @Query(value = """
           SELECT * FROM tasks
           WHERE board_list_id = :boardListId
             AND deleted_at IS NULL
             AND MATCH(title, description) AGAINST (:query IN NATURAL LANGUAGE MODE)
           """, nativeQuery = true)
    List<Task> searchByTitleOrDescription(@Param("boardListId") UUID boardListId, @Param("query") String query);

    @Query("SELECT t FROM Task t WHERE t.dueDate IS NOT NULL AND t.deletedAt IS NULL")
    List<Task> findAllWithDueDate();

    @Modifying
    @Query("""
           UPDATE Task t SET t.deletedAt = CURRENT_TIMESTAMP
           WHERE t.boardListId IN (
               SELECT bl.id FROM BoardList bl
               WHERE bl.boardId IN (SELECT b.id FROM Board b WHERE b.workspaceId = :workspaceId)
           )
           AND t.deletedAt IS NULL
           """)
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
