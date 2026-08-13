package com.minitrello.application.task;

import com.minitrello.application.project.ProjectAuthorizationService;
import com.minitrello.application.task.dto.CreateTagRequest;
import com.minitrello.application.task.dto.TagResponse;
import com.minitrello.domain.shared.exception.DuplicateResourceException;
import com.minitrello.domain.task.Tag;
import com.minitrello.domain.task.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final TaskMapper taskMapper;

    @Transactional
    public TagResponse createTag(UUID projectId, UUID callerId, CreateTagRequest request) {
        projectAuthorizationService.requireMembership(projectId, callerId);

        if (tagRepository.existsByProjectIdAndName(projectId, request.name().trim())) {
            throw new DuplicateResourceException("A tag with this name already exists in this project");
        }

        Tag tag = Tag.builder()
                .projectId(projectId)
                .name(request.name().trim())
                .color(request.color())
                .build();
        tag = tagRepository.save(tag);

        return taskMapper.toResponse(tag);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listForProject(UUID projectId, UUID callerId) {
        projectAuthorizationService.requireMembership(projectId, callerId);
        return tagRepository.findAllByProjectId(projectId).stream()
                .map(taskMapper::toResponse)
                .toList();
    }
}
