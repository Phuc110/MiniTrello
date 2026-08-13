package com.minitrello.domain.task;

import java.util.List;
import java.util.UUID;

public interface TaskAssigneeRepository {
    TaskAssignee save(TaskAssignee assignee);
    List<TaskAssignee> findAllByTaskId(UUID taskId);
    void deleteByTaskIdAndUserId(UUID taskId, UUID userId);
    boolean existsByTaskIdAndUserId(UUID taskId, UUID userId);
}
