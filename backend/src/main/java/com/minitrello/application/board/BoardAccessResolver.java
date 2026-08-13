package com.minitrello.application.board;

import com.minitrello.application.project.ProjectAuthorizationService;
import com.minitrello.domain.board.Board;
import com.minitrello.domain.board.BoardList;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.domain.board.BoardRepository;
import com.minitrello.domain.project.ProjectMember;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Board, BoardList, and Task all use plain FK columns rather than JPA
 * relations (consistent with Project/Workspace elsewhere in the codebase
 * — see Phase 6 notes), so resolving "which Project does this Task
 * ultimately belong to" means walking Task -> BoardList -> Board ->
 * Project explicitly. This class is the single place that walk happens,
 * so BoardService/BoardListService/TaskService don't each re-implement
 * (and potentially get wrong) the same three-hop lookup.
 */
@Component
@RequiredArgsConstructor
public class BoardAccessResolver {

    private final BoardRepository boardRepository;
    private final BoardListRepository boardListRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    public Board requireBoard(UUID boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
    }

    public BoardList requireBoardList(UUID boardListId) {
        return boardListRepository.findById(boardListId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardList", boardListId));
    }

    /** Checks membership for the project that owns the given board and returns the caller's membership row (role, etc.). */
    public ProjectMember requireMembershipForBoard(UUID boardId, UUID callerId) {
        Board board = requireBoard(boardId);
        return projectAuthorizationService.requireMembership(board.getProjectId(), callerId);
    }

    public ProjectMember requireMembershipForBoardList(UUID boardListId, UUID callerId) {
        BoardList boardList = requireBoardList(boardListId);
        Board board = requireBoard(boardList.getBoardId());
        return projectAuthorizationService.requireMembership(board.getProjectId(), callerId);
    }

    public UUID resolveProjectIdForBoardList(UUID boardListId) {
        BoardList boardList = requireBoardList(boardListId);
        return requireBoard(boardList.getBoardId()).getProjectId();
    }
}
