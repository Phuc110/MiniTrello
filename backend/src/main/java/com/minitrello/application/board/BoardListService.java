package com.minitrello.application.board;

import com.minitrello.application.board.dto.BoardListResponse;
import com.minitrello.application.board.dto.CreateBoardListRequest;
import com.minitrello.application.board.dto.MoveBoardListRequest;
import com.minitrello.domain.board.BoardList;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.domain.shared.PositionGenerator;
import com.minitrello.domain.shared.exception.BusinessRuleViolationException;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardListService {

    private final BoardListRepository boardListRepository;
    private final BoardAccessResolver boardAccessResolver;
    private final BoardMapper boardMapper;

    @Transactional
    public BoardListResponse createList(UUID boardId, UUID callerId, CreateBoardListRequest request) {
        boardAccessResolver.requireMembershipForBoard(boardId, callerId);

        String lastPosition = boardListRepository.findAllByBoardIdOrderByPosition(boardId).stream()
                .reduce((first, second) -> second) // last element
                .map(BoardList::getPosition)
                .orElse(null);

        BoardList boardList = BoardList.builder()
                .boardId(boardId)
                .name(request.name().trim())
                .position(lastPosition == null ? PositionGenerator.initial() : PositionGenerator.after(lastPosition))
                .build();
        boardList = boardListRepository.save(boardList);

        return boardMapper.toResponse(boardList);
    }

    @Transactional(readOnly = true)
    public List<BoardListResponse> listForBoard(UUID boardId, UUID callerId) {
        boardAccessResolver.requireMembershipForBoard(boardId, callerId);
        return boardListRepository.findAllByBoardIdOrderByPosition(boardId).stream()
                .map(boardMapper::toResponse)
                .toList();
    }

    /**
     * Drag-and-drop reorder. The client tells us the desired neighbors
     * (which list should end up immediately before/after this one); we
     * compute a new lexicographic position between them and write only
     * this ONE row — see PositionGenerator and the Phase 2 design
     * decision this implements.
     */
    @Transactional
    public BoardListResponse moveList(UUID boardListId, UUID callerId, MoveBoardListRequest request) {
        BoardList moving = boardAccessResolver.requireBoardList(boardListId);
        boardAccessResolver.requireMembershipForBoardList(boardListId, callerId);

        String prevPosition = resolveNeighborPosition(moving.getBoardId(), request.prevListId());
        String nextPosition = resolveNeighborPosition(moving.getBoardId(), request.nextListId());

        moving.setPosition(PositionGenerator.between(prevPosition, nextPosition));
        moving = boardListRepository.save(moving);

        return boardMapper.toResponse(moving);
    }

    @Transactional
    public void deleteList(UUID boardListId, UUID callerId) {
        BoardList boardList = boardAccessResolver.requireBoardList(boardListId);
        boardAccessResolver.requireMembershipForBoardList(boardListId, callerId);
        boardListRepository.softDeleteById(boardListId);
    }

    private String resolveNeighborPosition(UUID boardId, UUID neighborListId) {
        if (neighborListId == null) {
            return null;
        }
        BoardList neighbor = boardListRepository.findById(neighborListId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardList", neighborListId));
        if (!neighbor.getBoardId().equals(boardId)) {
            throw new BusinessRuleViolationException("Cannot reorder a list relative to a list on a different board");
        }
        return neighbor.getPosition();
    }
}
