package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.board.BoardList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardListJpaRepository extends JpaRepository<BoardList, UUID> {
    List<BoardList> findAllByBoardIdOrderByPositionAsc(UUID boardId);
    Optional<BoardList> findFirstByBoardIdAndPositionLessThanOrderByPositionDesc(UUID boardId, String position);
    Optional<BoardList> findFirstByBoardIdAndPositionGreaterThanOrderByPositionAsc(UUID boardId, String position);

    @Modifying
    @Query("""
           UPDATE BoardList bl SET bl.deletedAt = CURRENT_TIMESTAMP
           WHERE bl.boardId IN (SELECT b.id FROM Board b WHERE b.projectId = :projectId)
           AND bl.deletedAt IS NULL
           """)
    void softDeleteByProjectId(@Param("projectId") UUID projectId);

    @Modifying
    @Query("""
           UPDATE BoardList bl SET bl.deletedAt = CURRENT_TIMESTAMP
           WHERE bl.boardId IN (
               SELECT b.id FROM Board b
               WHERE b.projectId IN (SELECT p.id FROM Project p WHERE p.workspaceId = :workspaceId)
           )
           AND bl.deletedAt IS NULL
           """)
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}

