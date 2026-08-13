package com.minitrello.domain.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {

    Project save(Project project);

    Optional<Project> findById(UUID id);

    /**
     * Search projects visible to a given user (i.e. where a ProjectMember
     * row exists) within a workspace, optionally filtered by a name
     * substring. Pageable carries page/size/sort — see PageResponse for
     * how this is surfaced over the API.
     */
    Page<Project> searchForUser(UUID workspaceId, UUID userId, String nameFilter, Pageable pageable);

    void softDeleteByWorkspaceId(UUID workspaceId);
}
