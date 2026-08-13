package com.minitrello.domain.board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardRepository {

    Board save(Board board);

    Optional<Board> findById(UUID id);

    List<Board> findAllByProjectId(UUID projectId);

    void softDeleteByProjectId(UUID projectId);

    void softDeleteByWorkspaceId(UUID workspaceId);
}
