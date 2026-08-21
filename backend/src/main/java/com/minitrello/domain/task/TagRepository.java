package com.minitrello.domain.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository {
    Tag save(Tag tag);
    Optional<Tag> findById(UUID id);
    List<Tag> findAllByWorkspaceId(UUID workspaceId);
    boolean existsByWorkspaceIdAndName(UUID workspaceId, String name);
    void delete(Tag tag);
}
