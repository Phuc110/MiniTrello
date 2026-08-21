package com.minitrello.application.board;

import com.minitrello.domain.board.Board;
import com.minitrello.domain.board.BoardList;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.domain.board.BoardRepository;
import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import com.minitrello.domain.workspace.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Board and BoardList use plain FK columns. Resolving "which Workspace
 * does this Board/BoardList ultimately belong to" means walking
 * BoardList -> Board -> workspaceId. This class is the single place
 * that walk happens, so BoardService/BoardListService/TaskService don't
 * each re-implement the same lookup.
 */
@Component
@RequiredArgsConstructor
public class BoardAccessResolver {

    private final BoardRepository boardRepository;
    private final BoardListRepository boardListRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public Board requireBoard(UUID boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
    }

    public BoardList requireBoardList(UUID boardListId) {
        return boardListRepository.findById(boardListId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardList", boardListId));
    }

    /** Checks workspace membership for the workspace that owns the given board. */
    public void requireMembershipForBoard(UUID boardId, UUID callerId) {
        Board board = requireBoard(boardId);
        requireWorkspaceMembership(board.getWorkspaceId(), callerId);
    }

    public void requireMembershipForBoardList(UUID boardListId, UUID callerId) {
        BoardList boardList = requireBoardList(boardListId);
        Board board = requireBoard(boardList.getBoardId());
        requireWorkspaceMembership(board.getWorkspaceId(), callerId);
    }

    public UUID resolveWorkspaceIdForBoardList(UUID boardListId) {
        BoardList boardList = requireBoardList(boardListId);
        return requireBoard(boardList.getBoardId()).getWorkspaceId();
    }

    private void requireWorkspaceMembership(UUID workspaceId, UUID callerId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, callerId)) {
            throw new ForbiddenOperationException("You are not a member of this workspace");
        }
    }
}
