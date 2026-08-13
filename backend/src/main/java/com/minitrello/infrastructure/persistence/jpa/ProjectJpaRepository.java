package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.project.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProjectJpaRepository extends JpaRepository<Project, UUID> {

    /**
     * Joins through ProjectMember so results are inherently scoped to
     * projects the caller actually belongs to — this is the query-level
     * enforcement of "no cross-tenant data leakage" from the Phase 1
     * risk register: a caller can never see a project by guessing an ID
     * through this query path, only through findById + an explicit
     * membership check in the service layer.
     */
    @Query("""
           SELECT p FROM Project p
           JOIN ProjectMember pm ON pm.project = p
           WHERE p.workspaceId = :workspaceId
             AND pm.user.id = :userId
             AND (:nameFilter IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :nameFilter, '%')))
           """)
    Page<Project> searchForUser(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            @Param("nameFilter") String nameFilter,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Project p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.workspaceId = :workspaceId AND p.deletedAt IS NULL")
    void softDeleteByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}

