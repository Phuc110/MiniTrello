package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.task.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TagJpaRepository extends JpaRepository<Tag, UUID> {
    List<Tag> findAllByWorkspaceId(UUID workspaceId);
    boolean existsByWorkspaceIdAndName(UUID workspaceId, String name);
}
