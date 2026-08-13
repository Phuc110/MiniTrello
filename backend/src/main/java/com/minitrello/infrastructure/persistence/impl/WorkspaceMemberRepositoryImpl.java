package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.workspace.WorkspaceMember;
import com.minitrello.domain.workspace.WorkspaceMemberRepository;
import com.minitrello.infrastructure.persistence.jpa.WorkspaceMemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WorkspaceMemberRepositoryImpl implements WorkspaceMemberRepository {

    private final WorkspaceMemberJpaRepository jpaRepository;

    @Override
    public WorkspaceMember save(WorkspaceMember member) {
        return jpaRepository.save(member);
    }

    @Override
    public Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId) {
        return jpaRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId);
    }

    @Override
    public boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId) {
        return jpaRepository.existsByWorkspace_IdAndUser_Id(workspaceId, userId);
    }

    @Override
    public List<WorkspaceMember> findAllByWorkspaceId(UUID workspaceId) {
        return jpaRepository.findAllByWorkspace_Id(workspaceId);
    }

    @Override
    public List<WorkspaceMember> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUser_Id(userId);
    }

    @Override
    public void deleteByWorkspaceId(UUID workspaceId) {
        jpaRepository.deleteByWorkspace_Id(workspaceId);
    }
}

