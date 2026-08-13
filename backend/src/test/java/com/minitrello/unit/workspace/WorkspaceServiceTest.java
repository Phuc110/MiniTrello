package com.minitrello.unit.workspace;

import com.minitrello.application.workspace.WorkspaceMapper;
import com.minitrello.application.workspace.WorkspaceService;
import com.minitrello.application.workspace.dto.CreateWorkspaceRequest;
import com.minitrello.application.workspace.dto.WorkspaceResponse;
import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import com.minitrello.domain.user.SystemRole;
import com.minitrello.domain.user.User;
import com.minitrello.domain.user.UserRepository;
import com.minitrello.domain.workspace.Workspace;
import com.minitrello.domain.workspace.WorkspaceMemberRepository;
import com.minitrello.domain.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkspaceMapper workspaceMapper;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void createWorkspace_slugifiesName_andSavesOwnerAsMember() {
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("a@b.com").fullName("A B").systemRole(SystemRole.MEMBER).build();
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(workspaceRepository.existsBySlug(anyString())).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceMapper.toResponse(any(Workspace.class), anyBoolean())).thenReturn(
                new WorkspaceResponse(UUID.randomUUID(), "Acme Corp!", "acme-corp", ownerId, null, true));

        workspaceService.createWorkspace(ownerId, new CreateWorkspaceRequest("Acme Corp!"));

        ArgumentCaptor<Workspace> captor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(captor.capture());
        // Non-alphanumeric characters are stripped and collapsed into hyphens.
        assertThat(captor.getValue().getSlug()).isEqualTo("acme-corp");
        assertThat(captor.getValue().getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void createWorkspace_dedupesSlugOnCollision() {
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("a@b.com").fullName("A B").systemRole(SystemRole.MEMBER).build();
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        // First candidate slug is taken, second (with -2 suffix) is free.
        when(workspaceRepository.existsBySlug("acme")).thenReturn(true);
        when(workspaceRepository.existsBySlug("acme-2")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceMapper.toResponse(any(Workspace.class), anyBoolean())).thenReturn(
                new WorkspaceResponse(UUID.randomUUID(), "Acme", "acme-2", ownerId, null, true));

        workspaceService.createWorkspace(ownerId, new CreateWorkspaceRequest("Acme"));

        ArgumentCaptor<Workspace> captor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("acme-2");
    }

    @Test
    void getWorkspace_rejectsNonMembers() {
        UUID workspaceId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        when(workspaceRepository.findById(workspaceId))
                .thenReturn(Optional.of(Workspace.builder().id(workspaceId).build()));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, callerId)).thenReturn(false);

        assertThatThrownBy(() -> workspaceService.getWorkspace(workspaceId, callerId))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
