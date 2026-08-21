package com.minitrello.application.task;

import com.minitrello.application.task.dto.CreateTagRequest;
import com.minitrello.application.task.dto.TagResponse;
import com.minitrello.domain.shared.exception.DuplicateResourceException;
import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import com.minitrello.domain.task.Tag;
import com.minitrello.domain.task.TagRepository;
import com.minitrello.domain.task.TaskTagRepository;
import com.minitrello.domain.workspace.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TaskTagRepository taskTagRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public TagResponse createTag(UUID workspaceId, UUID callerId, CreateTagRequest request) {
        requireWorkspaceMembership(workspaceId, callerId);

        if (tagRepository.existsByWorkspaceIdAndName(workspaceId, request.name().trim())) {
            throw new DuplicateResourceException("A tag with this name already exists in this workspace");
        }

        Tag tag = Tag.builder()
                .workspaceId(workspaceId)
                .name(request.name().trim())
                .color(request.color())
                .build();
        tag = tagRepository.save(tag);

        return taskMapper.toResponse(tag);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listForWorkspace(UUID workspaceId, UUID callerId) {
        requireWorkspaceMembership(workspaceId, callerId);
        return tagRepository.findAllByWorkspaceId(workspaceId).stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteTag(UUID tagId, UUID callerId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", tagId));
        requireWorkspaceMembership(tag.getWorkspaceId(), callerId);

        // No JPA associations — cascade the join-table cleanup manually first.
        taskTagRepository.deleteAllByTagId(tagId);
        tagRepository.delete(tag);
    }

    private void requireWorkspaceMembership(UUID workspaceId, UUID callerId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, callerId)) {
            throw new ForbiddenOperationException("You are not a member of this workspace");
        }
    }
}
