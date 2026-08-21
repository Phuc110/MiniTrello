package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.workspace.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberJpaRepository extends JpaRepository<WorkspaceMember, UUID> {
    Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);

    @Query("SELECT CASE WHEN COUNT(wm) > 0 THEN true ELSE false END FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId AND wm.user.id = :userId AND wm.workspace.deletedAt IS NULL")
    boolean existsByWorkspace_IdAndUser_Id(@Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);

    List<WorkspaceMember> findAllByWorkspace_Id(UUID workspaceId);

    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.user.id = :userId AND wm.workspace.deletedAt IS NULL")
    List<WorkspaceMember> findAllByUser_Id(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId")
    void deleteByWorkspace_Id(@Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("DELETE FROM WorkspaceMember wm WHERE wm.workspace.id = :workspaceId AND wm.user.id = :userId")
    void deleteByWorkspace_IdAndUser_Id(@Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);
}

