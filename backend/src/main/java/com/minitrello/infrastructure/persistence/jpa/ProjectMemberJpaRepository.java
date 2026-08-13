package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.project.ProjectMember;
import com.minitrello.domain.project.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberJpaRepository extends JpaRepository<ProjectMember, UUID> {
    Optional<ProjectMember> findByProject_IdAndUser_Id(UUID projectId, UUID userId);
    List<ProjectMember> findAllByProject_Id(UUID projectId);
    long countByProject_IdAndRole(UUID projectId, ProjectRole role);

    @Modifying
    @Query("DELETE FROM ProjectMember pm WHERE pm.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);

    @Modifying
    @Query("DELETE FROM ProjectMember pm WHERE pm.project.id IN (SELECT p.id FROM Project p WHERE p.workspaceId = :workspaceId)")
    void deleteByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}

