package com.minitrello.unit.auth;

import com.minitrello.application.auth.AuthService;
import com.minitrello.application.auth.RefreshTokenService;
import com.minitrello.application.auth.UserMapper;
import com.minitrello.application.auth.dto.RegisterRequest;
import com.minitrello.application.auth.dto.UserResponse;
import com.minitrello.domain.shared.exception.DuplicateResourceException;
import com.minitrello.domain.user.SystemRole;
import com.minitrello.domain.user.User;
import com.minitrello.domain.user.UserRepository;
import com.minitrello.infrastructure.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests — every collaborator is mocked, so these run with no
 * Spring context and no database, per the domain/application layers'
 * "testable in isolation" goal from Phase 2.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMinutes", 15L);
    }

    @Test
    void register_shouldCreateUser_whenEmailIsNotTaken() {
        RegisterRequest request = new RegisterRequest("new.user@example.com", "SecurePass1", "New User");
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .fullName(request.fullName())
                .systemRole(SystemRole.MEMBER)
                .build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.issue(savedUser)).thenReturn("raw-refresh-token");
        when(userMapper.toResponse(savedUser)).thenReturn(
                new UserResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), SystemRole.MEMBER));

        AuthService.AuthResult result = authService.register(request);

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.rawRefreshToken()).isEqualTo("raw-refresh-token");
        assertThat(result.response().user().email()).isEqualTo(request.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("taken@example.com", "SecurePass1", "Someone");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void refresh_shouldDelegateRotationToRefreshTokenService() {
        User user = User.builder().id(UUID.randomUUID()).email("a@b.com").fullName("A B")
                .systemRole(SystemRole.MEMBER).build();
        when(refreshTokenService.rotate("old-raw-token"))
                .thenReturn(new RefreshTokenService.RotationResult(user, "new-raw-token"));
        when(jwtProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
        when(userMapper.toResponse(user)).thenReturn(
                new UserResponse(user.getId(), user.getEmail(), user.getFullName(), SystemRole.MEMBER));

        AuthService.AuthResult result = authService.refresh("old-raw-token");

        assertThat(result.rawRefreshToken()).isEqualTo("new-raw-token");
        verify(refreshTokenService).rotate("old-raw-token");
    }
}
