package com.minitrello.domain.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository {

    ProjectMember save(ProjectMember member);

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMember> findAllByProjectId(UUID projectId);

    void delete(ProjectMember member);

    void deleteByProjectId(UUID projectId);

    void deleteByWorkspaceId(UUID workspaceId);

    long countByProjectIdAndRole(UUID projectId, ProjectRole role);
}

