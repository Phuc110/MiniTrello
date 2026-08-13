package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BoardJpaRepository extends JpaRepository<Board, UUID> {
    List<Board> findAllByProjectId(UUID projectId);

    @Modifying
    @Query("UPDATE Board b SET b.deletedAt = CURRENT_TIMESTAMP WHERE b.projectId = :projectId AND b.deletedAt IS NULL")
    void softDeleteByProjectId(@Param("projectId") UUID projectId);

    @Modifying
    @Query("""
           UPDATE Board b SET b.deletedAt = CURRENT_TIMESTAMP
           WHERE b.projectId IN (SELECT p.id FROM Project p WHERE p.workspaceId = :workspaceId)
           AND b.deletedAt IS NULL
           """)
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}

