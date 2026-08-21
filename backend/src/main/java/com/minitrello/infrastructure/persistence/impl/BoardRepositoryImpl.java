package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.board.Board;
import com.minitrello.domain.board.BoardRepository;
import com.minitrello.infrastructure.persistence.jpa.BoardJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepository {

    private final BoardJpaRepository jpaRepository;

    @Override
    public Board save(Board board) {
        return jpaRepository.save(board);
    }

    @Override
    public Optional<Board> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Board> findAllByWorkspaceId(UUID workspaceId) {
        return jpaRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public void softDeleteByWorkspaceId(UUID workspaceId) {
        jpaRepository.softDeleteByWorkspaceId(workspaceId);
    }
}
