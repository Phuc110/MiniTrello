package com.minitrello.application.project;

import com.minitrello.application.project.dto.ProjectMemberResponse;
import com.minitrello.application.project.dto.ProjectResponse;
import com.minitrello.domain.project.Project;
import com.minitrello.domain.project.ProjectMember;
import com.minitrello.domain.project.ProjectRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    /**
     * callerRole can't be derived from Project alone (it depends on WHO
     * is asking), so it's mapped separately here rather than pulled from
     * the entity graph — keeps this an explicit, obviously-intentional
     * parameter rather than a mapper "guessing" at a relationship.
     */
    @Mapping(target = "callerRole", source = "callerRole")
    ProjectResponse toResponse(Project project, ProjectRole callerRole);

    @Mapping(target = "userId", source = "member.user.id")
    @Mapping(target = "email", source = "member.user.email")
    @Mapping(target = "fullName", source = "member.user.fullName")
    @Mapping(target = "role", source = "member.role")
    ProjectMemberResponse toMemberResponse(ProjectMember member);
}
