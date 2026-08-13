package com.minitrello.application.auth;

import com.minitrello.application.auth.dto.UserResponse;
import com.minitrello.domain.user.User;
import org.mapstruct.Mapper;

/**
 * MapStruct generates the implementation at compile time — no reflection,
 * no runtime mapping cost. componentModel="spring" makes the generated
 * impl a Spring bean we can @RequiredArgsConstructor-inject like any
 * other collaborator.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
