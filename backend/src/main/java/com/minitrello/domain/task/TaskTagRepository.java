package com.minitrello.domain.task;

import java.util.List;
import java.util.UUID;

public interface TaskTagRepository {
    TaskTag save(TaskTag taskTag);
    List<TaskTag> findAllByTaskId(UUID taskId);
    void deleteByTaskIdAndTagId(UUID taskId, UUID tagId);
    boolean existsByTaskIdAndTagId(UUID taskId, UUID tagId);
}
