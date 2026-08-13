package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.project.ProjectMember;
import com.minitrello.domain.project.ProjectMemberRepository;
import com.minitrello.domain.project.ProjectRole;
import com.minitrello.infrastructure.persistence.jpa.ProjectMemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProjectMemberRepositoryImpl implements ProjectMemberRepository {

    private final ProjectMemberJpaRepository jpaRepository;

    @Override
    public ProjectMember save(ProjectMember member) {
        return jpaRepository.save(member);
    }

    @Override
    public Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId) {
        return jpaRepository.findByProject_IdAndUser_Id(projectId, userId);
    }

    @Override
    public List<ProjectMember> findAllByProjectId(UUID projectId) {
        return jpaRepository.findAllByProject_Id(projectId);
    }

    @Override
    public void delete(ProjectMember member) {
        jpaRepository.delete(member);
    }

    @Override
    public void deleteByProjectId(UUID projectId) {
        jpaRepository.deleteByProjectId(projectId);
    }

    @Override
    public void deleteByWorkspaceId(UUID workspaceId) {
        jpaRepository.deleteByWorkspaceId(workspaceId);
    }

    @Override
    public long countByProjectIdAndRole(UUID projectId, ProjectRole role) {
        return jpaRepository.countByProject_IdAndRole(projectId, role);
    }
}

