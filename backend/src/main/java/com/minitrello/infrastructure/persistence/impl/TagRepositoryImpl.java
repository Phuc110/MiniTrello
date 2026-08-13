package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.task.Tag;
import com.minitrello.domain.task.TagRepository;
import com.minitrello.infrastructure.persistence.jpa.TagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepository {

    private final TagJpaRepository jpaRepository;

    @Override
    public Tag save(Tag tag) {
        return jpaRepository.save(tag);
    }

    @Override
    public Optional<Tag> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Tag> findAllByProjectId(UUID projectId) {
        return jpaRepository.findAllByProjectId(projectId);
    }

    @Override
    public boolean existsByProjectIdAndName(UUID projectId, String name) {
        return jpaRepository.existsByProjectIdAndName(projectId, name);
    }
}
