package com.minitrello.application.board;

import com.minitrello.application.board.dto.BoardResponse;
import com.minitrello.application.board.dto.CreateBoardRequest;
import com.minitrello.application.project.ProjectAuthorizationService;
import com.minitrello.domain.board.Board;
import com.minitrello.domain.board.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final BoardMapper boardMapper;

    @Transactional
    public BoardResponse createBoard(UUID projectId, UUID callerId, CreateBoardRequest request) {
        // Any project member can create a board — it's a workspace
        // organizational tool, not a privileged action like deleting the
        // project itself.
        projectAuthorizationService.requireMembership(projectId, callerId);

        Board board = Board.builder()
                .projectId(projectId)
                .name(request.name().trim())
                .build();
        board = boardRepository.save(board);

        return boardMapper.toResponse(board);
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> listBoards(UUID projectId, UUID callerId) {
        projectAuthorizationService.requireMembership(projectId, callerId);
        return boardRepository.findAllByProjectId(projectId).stream()
                .map(boardMapper::toResponse)
                .toList();
    }
}
