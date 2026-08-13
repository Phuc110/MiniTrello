package com.minitrello.application.auth;

import com.minitrello.application.auth.dto.AuthResponse;
import com.minitrello.application.auth.dto.LoginRequest;
import com.minitrello.application.auth.dto.RegisterRequest;
import com.minitrello.domain.shared.exception.DuplicateResourceException;
import com.minitrello.domain.user.SystemRole;
import com.minitrello.domain.user.User;
import com.minitrello.domain.user.UserRepository;
import com.minitrello.infrastructure.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * Orchestrates the authentication use cases. Deliberately thin: it
 * delegates password verification to Spring Security's
 * AuthenticationManager (so we get its brute-force-resistant timing
 * behavior for free) and refresh-token mechanics to RefreshTokenService,
 * rather than reimplementing either here.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Value("${app.jwt.access-token-expiration-minutes}")
    private long accessTokenExpirationMinutes;

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .systemRole(SystemRole.MEMBER)
                .build();

        user = userRepository.save(user);

        return issueTokensFor(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        // Delegates to Spring Security so failed attempts get its standard
        // constant-time credential comparison — we never compare the
        // raw/hashed password ourselves.
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return issueTokensFor(user);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);
        return buildAuthResult(rotation.user(), rotation.rawRefreshToken());
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthResult issueTokensFor(User user) {
        String rawRefreshToken = refreshTokenService.issue(user);
        return buildAuthResult(user, rawRefreshToken);
    }

    private AuthResult buildAuthResult(User user, String rawRefreshToken) {
        String accessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getSystemRole().name());

        long expiresInSeconds = Duration.ofMinutes(accessTokenExpirationMinutes).toSeconds();

        AuthResponse response = new AuthResponse(accessToken, expiresInSeconds, userMapper.toResponse(user));
        return new AuthResult(response, rawRefreshToken);
    }

    /** Carries the refresh token alongside the API-facing AuthResponse so AuthController can set it as an httpOnly cookie without putting it in the JSON body. */
    public record AuthResult(AuthResponse response, String rawRefreshToken) {
    }
}
