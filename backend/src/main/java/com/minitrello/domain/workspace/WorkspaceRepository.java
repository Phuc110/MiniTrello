package com.minitrello.domain.workspace;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository {

    Workspace save(Workspace workspace);

    Optional<Workspace> findById(UUID id);

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
