package com.minitrello.application.workspace;

import com.minitrello.application.workspace.dto.WorkspaceResponse;
import com.minitrello.domain.workspace.Workspace;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {
    WorkspaceResponse toResponse(Workspace workspace, boolean canDelete);
}
