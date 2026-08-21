package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.board.BoardList;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.infrastructure.persistence.jpa.BoardListJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BoardListRepositoryImpl implements BoardListRepository {

    private final BoardListJpaRepository jpaRepository;

    @Override
    public BoardList save(BoardList boardList) {
        return jpaRepository.save(boardList);
    }

    @Override
    public Optional<BoardList> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<BoardList> findAllByBoardIdOrderByPosition(UUID boardId) {
        return jpaRepository.findAllByBoardIdOrderByPositionAsc(boardId);
    }

    @Override
    public Optional<BoardList> findFirstByBoardIdAndPositionLessThanOrderByPositionDesc(UUID boardId, String position) {
        return jpaRepository.findFirstByBoardIdAndPositionLessThanOrderByPositionDesc(boardId, position);
    }

    @Override
    public Optional<BoardList> findFirstByBoardIdAndPositionGreaterThanOrderByPositionAsc(UUID boardId, String position) {
        return jpaRepository.findFirstByBoardIdAndPositionGreaterThanOrderByPositionAsc(boardId, position);
    }

    @Override
    public void softDeleteByWorkspaceId(UUID workspaceId) {
        jpaRepository.softDeleteByWorkspaceId(workspaceId);
    }

    @Override
    public void softDeleteById(UUID id) {
        jpaRepository.softDeleteById(id);
    }
}
