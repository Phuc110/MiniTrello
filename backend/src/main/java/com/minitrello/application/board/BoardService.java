package com.minitrello.application.board;

import com.minitrello.application.board.dto.BoardResponse;
import com.minitrello.application.board.dto.CreateBoardRequest;
import com.minitrello.domain.board.Board;
import com.minitrello.domain.board.BoardList;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.domain.board.BoardRepository;
import com.minitrello.domain.shared.PositionGenerator;
import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import com.minitrello.domain.workspace.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardListRepository boardListRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final BoardMapper boardMapper;

    @Transactional
    public BoardResponse createBoard(UUID workspaceId, UUID callerId, CreateBoardRequest request) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, callerId)) {
            throw new ForbiddenOperationException("You must be a workspace member to create a board in it");
        }

        Board board = Board.builder()
                .workspaceId(workspaceId)
                .name(request.name().trim())
                .build();
        board = boardRepository.save(board);

        // Auto-generate 3 default lists: To Do, In Progress, Done
        String pos1 = PositionGenerator.initial();
        String pos2 = PositionGenerator.after(pos1);
        String pos3 = PositionGenerator.after(pos2);

        boardListRepository.save(BoardList.builder()
                .boardId(board.getId()).name("To Do").position(pos1).build());
        boardListRepository.save(BoardList.builder()
                .boardId(board.getId()).name("In Progress").position(pos2).build());
        boardListRepository.save(BoardList.builder()
                .boardId(board.getId()).name("Done").position(pos3).build());

        return boardMapper.toResponse(board);
    }

    @Transactional(readOnly = true)
    public BoardResponse getBoard(UUID boardId, UUID callerId) {
        Board board = requireBoard(boardId);
        requireWorkspaceMembership(board.getWorkspaceId(), callerId);
        return boardMapper.toResponse(board);
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> listBoards(UUID workspaceId, UUID callerId) {
        requireWorkspaceMembership(workspaceId, callerId);
        return boardRepository.findAllByWorkspaceId(workspaceId).stream()
                .map(boardMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteBoard(UUID boardId, UUID callerId) {
        Board board = requireBoard(boardId);
        requireWorkspaceMembership(board.getWorkspaceId(), callerId);
        board.softDelete();
        boardRepository.save(board);
    }

    private Board requireBoard(UUID boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
    }

    private void requireWorkspaceMembership(UUID workspaceId, UUID callerId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, callerId)) {
            throw new ForbiddenOperationException("You are not a member of this workspace");
        }
    }
}
