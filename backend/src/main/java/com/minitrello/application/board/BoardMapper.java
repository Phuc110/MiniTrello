package com.minitrello.application.board;

import com.minitrello.application.board.dto.BoardListResponse;
import com.minitrello.application.board.dto.BoardResponse;
import com.minitrello.domain.board.Board;
import com.minitrello.domain.board.BoardList;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BoardMapper {
    BoardResponse toResponse(Board board);
    BoardListResponse toResponse(BoardList boardList);
}
