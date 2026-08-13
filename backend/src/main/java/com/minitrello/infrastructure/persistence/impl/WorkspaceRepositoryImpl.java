package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.workspace.Workspace;
import com.minitrello.domain.workspace.WorkspaceRepository;
import com.minitrello.infrastructure.persistence.jpa.WorkspaceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WorkspaceRepositoryImpl implements WorkspaceRepository {

    private final WorkspaceJpaRepository jpaRepository;

    @Override
    public Workspace save(Workspace workspace) {
        return jpaRepository.save(workspace);
    }

    @Override
    public Optional<Workspace> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Workspace> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlug(slug);
    }
}
