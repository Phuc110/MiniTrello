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

@Mapper(componentModel = "spring")
public interface TaskMapper {

    TagResponse toResponse(Tag tag);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    TaskAssigneeResponse toResponse(TaskAssignee assignee);

    /**
     * Assembled explicitly (not auto-derived by MapStruct) because
     * assignees/tags come from separate join-table queries in
     * TaskService, not from a navigable relation on Task itself — see
     * the plain-FK design note on BoardAccessResolver.
     */
    default TaskResponse toFullResponse(Task task, List<TaskAssigneeResponse> assignees, List<TagResponse> tags) {
        return new TaskResponse(
                task.getId(),
                task.getBoardListId(),
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
