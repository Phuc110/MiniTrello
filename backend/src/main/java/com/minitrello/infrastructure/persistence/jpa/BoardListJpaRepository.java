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
           WHERE bl.boardId IN (SELECT b.id FROM Board b WHERE b.workspaceId = :workspaceId)
           AND bl.deletedAt IS NULL
           """)
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("UPDATE BoardList bl SET bl.deletedAt = CURRENT_TIMESTAMP WHERE bl.id = :id AND bl.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id);
}
