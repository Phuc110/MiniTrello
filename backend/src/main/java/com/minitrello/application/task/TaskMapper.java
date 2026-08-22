package com.minitrello.application.task;

import com.minitrello.application.task.dto.TagResponse;
import com.minitrello.application.task.dto.TaskAssigneeResponse;
import com.minitrello.application.task.dto.TaskResponse;
import com.minitrello.domain.task.Tag;
import com.minitrello.domain.task.Task;
import com.minitrello.domain.task.TaskAssignee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    TagResponse toResponse(Tag tag);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    TaskAssigneeResponse toResponse(TaskAssignee assignee);

    /**
     * Assembled explicitly (not auto-derived by MapStruct) because
     * workspaceId/assignees/tags come from separate lookups in TaskService,
     * not from navigable relations on Task itself — see the plain-FK design
     * note on BoardAccessResolver. workspaceId lets clients open the task
     * from anywhere (e.g. My Tasks) and still load tag pickers / members.
     */
    default TaskResponse toFullResponse(Task task, UUID workspaceId, List<TaskAssigneeResponse> assignees, List<TagResponse> tags) {
        return new TaskResponse(
                task.getId(),
                task.getBoardListId(),
                workspaceId,
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getPosition(),
                task.getDueDate(),
                assignees,
                tags,
                task.getCreatedAt()
        );
    }
}
