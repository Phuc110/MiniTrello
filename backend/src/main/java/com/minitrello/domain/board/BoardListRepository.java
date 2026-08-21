package com.minitrello.domain.board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardListRepository {

    BoardList save(BoardList boardList);

    Optional<BoardList> findById(UUID id);

    /** Ordered by position — this is exactly the order the Kanban UI should render columns in. */
    List<BoardList> findAllByBoardIdOrderByPosition(UUID boardId);

    /** Used by PositionGenerator to find the neighbors of a target position when computing where a moved list should land. */
    Optional<BoardList> findFirstByBoardIdAndPositionLessThanOrderByPositionDesc(UUID boardId, String position);

    Optional<BoardList> findFirstByBoardIdAndPositionGreaterThanOrderByPositionAsc(UUID boardId, String position);

    void softDeleteByWorkspaceId(UUID workspaceId);

    void softDeleteById(UUID id);
}
