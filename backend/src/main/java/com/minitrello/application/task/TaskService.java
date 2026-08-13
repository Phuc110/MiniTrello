package com.minitrello.application.task;

import com.minitrello.application.board.BoardAccessResolver;
import com.minitrello.application.task.dto.CreateTaskRequest;
import com.minitrello.application.task.dto.MoveTaskRequest;
import com.minitrello.application.task.dto.TagResponse;
import com.minitrello.application.task.dto.TaskAssigneeResponse;
import com.minitrello.application.task.dto.TaskResponse;
import com.minitrello.application.task.dto.UpdateTaskRequest;
import com.minitrello.domain.project.ProjectMember;
import com.minitrello.domain.shared.PositionGenerator;
import com.minitrello.domain.shared.exception.BusinessRuleViolationException;
import com.minitrello.domain.shared.exception.DuplicateResourceException;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import com.minitrello.domain.task.Priority;
import com.minitrello.domain.task.Tag;
import com.minitrello.domain.task.TagRepository;
import com.minitrello.domain.task.Task;
import com.minitrello.domain.task.TaskAssignee;
import com.minitrello.domain.task.TaskAssigneeRepository;
import com.minitrello.domain.task.TaskRepository;
import com.minitrello.domain.task.TaskTag;
import com.minitrello.domain.task.TaskTagRepository;
import com.minitrello.domain.user.User;
import com.minitrello.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final TagRepository tagRepository;
    private final TaskTagRepository taskTagRepository;
    private final UserRepository userRepository;
    private final BoardAccessResolver boardAccessResolver;
    private final TaskMapper taskMapper;

    @Transactional
    public TaskResponse createTask(UUID boardListId, UUID callerId, CreateTaskRequest request) {
        boardAccessResolver.requireMembershipForBoardList(boardListId, callerId);

        String lastPosition = taskRepository.findAllByBoardListIdOrderByPosition(boardListId).stream()
                .reduce((first, second) -> second)
                .map(Task::getPosition)
                .orElse(null);

        Task task = Task.builder()
                .boardListId(boardListId)
                .title(request.title().trim())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : Priority.MEDIUM)
                .dueDate(request.dueDate())
                .position(lastPosition == null ? PositionGenerator.initial() : PositionGenerator.after(lastPosition))
                .build();
        task = taskRepository.save(task);

        return toFullResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId, UUID callerId) {
        Task task = requireTask(taskId);
        requireMembershipForTask(task, callerId);
        return toFullResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listForBoardList(UUID boardListId, UUID callerId) {
        boardAccessResolver.requireMembershipForBoardList(boardListId, callerId);
        return taskRepository.findAllByBoardListIdOrderByPosition(boardListId).stream()
                .map(this::toFullResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> search(UUID boardListId, UUID callerId, String query) {
        boardAccessResolver.requireMembershipForBoardList(boardListId, callerId);
        return taskRepository.searchByTitleOrDescription(boardListId, query).stream()
                .map(this::toFullResponse)
                .toList();
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UUID callerId, UpdateTaskRequest request) {
        Task task = requireTask(taskId);
        requireMembershipForTask(task, callerId);

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task = taskRepository.save(task);

        return toFullResponse(task);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID callerId) {
        Task task = requireTask(taskId);
        requireMembershipForTask(task, callerId);
        task.softDelete();
        taskRepository.save(task);
    }

    /**
     * Drag-and-drop move — can reposition within the same list OR move the
     * task to a different list entirely (both are the same operation from
     * the client's point of view: "the card is now here, between these two
     * neighbors"). See PositionGenerator / MoveTaskRequest for the
     * neighbor-based positioning contract.
     */
    @Transactional
    public TaskResponse moveTask(UUID taskId, UUID callerId, MoveTaskRequest request) {
        Task task = requireTask(taskId);
        requireMembershipForTask(task, callerId);

        // A task may only move between lists within the SAME project —
        // resolving the destination's project and requiring membership
        // there too (even though it'll always be the caller, since we
        // enforce same-project) closes off a cross-tenant write path:
        // without this, a caller could otherwise move a task into a
        // board_list_id belonging to a project they have no access to,
        // which is exactly the leakage risk flagged in the Phase 1 risk
        // register.
        UUID sourceProjectId = boardAccessResolver.resolveProjectIdForBoardList(task.getBoardListId());
        UUID targetProjectId = boardAccessResolver.resolveProjectIdForBoardList(request.targetBoardListId());
        if (!sourceProjectId.equals(targetProjectId)) {
            throw new BusinessRuleViolationException("A task cannot be moved to a list in a different project");
        }
        boardAccessResolver.requireMembershipForBoardList(request.targetBoardListId(), callerId);

        String prevPosition = resolveNeighborPosition(request.targetBoardListId(), request.prevTaskId());
        String nextPosition = resolveNeighborPosition(request.targetBoardListId(), request.nextTaskId());

        task.setBoardListId(request.targetBoardListId());
        task.setPosition(PositionGenerator.between(prevPosition, nextPosition));
        task = taskRepository.save(task);

        return toFullResponse(task);
    }

    @Transactional
    public void assignUser(UUID taskId, UUID callerId, UUID assigneeUserId) {
        Task task = requireTask(taskId);
        requireMembershipForTask(task, callerId);

        User assignee = userRepository.findById(assigneeUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", assigneeUserId));

        if (taskAssigneeRepository.existsByTaskIdAndUserId(taskId, assigneeUserId)) {
            throw new DuplicateResourceException("User is already assigned to this task");
        }

        taskAssigneeRepository.save(TaskAssignee.builder().task(task).user(assignee).build());
    }

    @Transactional
    public void unassignUser(UUID taskId, UUID callerId, UUID assigneeUserId) {
        Task task = requireTask(taskId);
        requireMembershipForTask(task, callerId);
        taskAssigneeRepository.deleteByTaskIdAndUserId(taskId, assigneeUserId);
    }

    @Transactional
    public void addTag(UUID taskId, UUID callerId, UUID tagId) {
        Task task = requireTask(taskId);
        requireMembershipForTask(task, callerId);

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", tagId));

        if (taskTagRepository.existsByTaskIdAndTagId(taskId, tagId)) {
            throw new DuplicateResourceException("Tag is already applied to this task");
        }

        taskTagRepository.save(TaskTag.builder().task(task).tag(tag).build());
    }

    @Transactional
    public void removeTag(UUID taskId, UUID callerId, UUID tagId) {
        Task task = requireTask(taskId);
        requireMembershipForTask(task, callerId);
        taskTagRepository.deleteByTaskIdAndTagId(taskId, tagId);
    }

    private String resolveNeighborPosition(UUID targetBoardListId, UUID neighborTaskId) {
        if (neighborTaskId == null) {
            return null;
        }
        Task neighbor = taskRepository.findById(neighborTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", neighborTaskId));
        if (!neighbor.getBoardListId().equals(targetBoardListId)) {
            throw new BusinessRuleViolationException("Cannot position a task relative to a task in a different list");
        }
        return neighbor.getPosition();
    }

    private TaskResponse toFullResponse(Task task) {
        List<TaskAssigneeResponse> assignees = taskAssigneeRepository.findAllByTaskId(task.getId()).stream()
                .map(taskMapper::toResponse)
                .toList();
        List<TagResponse> tags = taskTagRepository.findAllByTaskId(task.getId()).stream()
                .map(taskTag -> taskMapper.toResponse(taskTag.getTag()))
                .toList();
        return taskMapper.toFullResponse(task, assignees, tags);
    }

    private Task requireTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    }

    private ProjectMember requireMembershipForTask(Task task, UUID callerId) {
        return boardAccessResolver.requireMembershipForBoardList(task.getBoardListId(), callerId);
    }
}
