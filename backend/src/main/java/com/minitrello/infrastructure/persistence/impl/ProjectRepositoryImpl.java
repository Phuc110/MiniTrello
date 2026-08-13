package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.project.Project;
import com.minitrello.domain.project.ProjectRepository;
import com.minitrello.infrastructure.persistence.jpa.ProjectJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepository {

    private final ProjectJpaRepository jpaRepository;

    @Override
    public Project save(Project project) {
        return jpaRepository.save(project);
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Project> searchForUser(UUID workspaceId, UUID userId, String nameFilter, Pageable pageable) {
        return jpaRepository.searchForUser(workspaceId, userId, nameFilter, pageable);
    }

    @Override
    public void softDeleteByWorkspaceId(UUID workspaceId) {
        jpaRepository.softDeleteByWorkspaceId(workspaceId);
    }
}
