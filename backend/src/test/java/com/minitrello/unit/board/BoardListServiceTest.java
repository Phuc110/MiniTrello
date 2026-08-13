package com.minitrello.unit.board;

import com.minitrello.application.board.BoardAccessResolver;
import com.minitrello.application.board.BoardListService;
import com.minitrello.application.board.BoardMapper;
import com.minitrello.application.board.dto.CreateBoardListRequest;
import com.minitrello.application.board.dto.MoveBoardListRequest;
import com.minitrello.domain.board.BoardList;
import com.minitrello.domain.board.BoardListRepository;
import com.minitrello.domain.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardListServiceTest {

    @Mock private BoardListRepository boardListRepository;
    @Mock private BoardAccessResolver boardAccessResolver;
    @Mock private BoardMapper boardMapper;

    @InjectMocks
    private BoardListService boardListService;

    @Test
    void createList_onEmptyBoard_usesInitialPosition() {
        UUID boardId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        when(boardListRepository.findAllByBoardIdOrderByPosition(boardId)).thenReturn(List.of());
        when(boardListRepository.save(any(BoardList.class))).thenAnswer(inv -> inv.getArgument(0));

        boardListService.createList(boardId, callerId, new CreateBoardListRequest("To Do"));

        ArgumentCaptor<BoardList> captor = ArgumentCaptor.forClass(BoardList.class);
        verify(boardListRepository).save(captor.capture());
        assertThat(captor.getValue().getPosition()).isNotBlank();
    }

    @Test
    void createList_onNonEmptyBoard_appendsAfterLastPosition() {
        UUID boardId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        BoardList existing = BoardList.builder().boardId(boardId).name("To Do").position("m").build();
        when(boardListRepository.findAllByBoardIdOrderByPosition(boardId)).thenReturn(List.of(existing));
        when(boardListRepository.save(any(BoardList.class))).thenAnswer(inv -> inv.getArgument(0));

        boardListService.createList(boardId, callerId, new CreateBoardListRequest("In Progress"));

        ArgumentCaptor<BoardList> captor = ArgumentCaptor.forClass(BoardList.class);
        verify(boardListRepository).save(captor.capture());
        assertThat(captor.getValue().getPosition()).isGreaterThan("m");
    }

    @Test
    void moveList_rejectsNeighborFromADifferentBoard() {
        UUID boardId = UUID.randomUUID();
        UUID otherBoardId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        UUID foreignListId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();

        BoardList moving = BoardList.builder().id(listId).boardId(boardId).name("Done").position("t").build();
        BoardList foreignNeighbor = BoardList.builder().id(foreignListId).boardId(otherBoardId).position("a").build();

        when(boardAccessResolver.requireBoardList(listId)).thenReturn(moving);
        when(boardListRepository.findById(foreignListId)).thenReturn(Optional.of(foreignNeighbor));

        assertThatThrownBy(() -> boardListService.moveList(
                listId, callerId, new MoveBoardListRequest(foreignListId, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
