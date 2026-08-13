package com.minitrello.presentation.controller;

import com.minitrello.application.auth.AuthService;
import com.minitrello.application.auth.dto.AuthResponse;
import com.minitrello.application.auth.dto.LoginRequest;
import com.minitrello.application.auth.dto.RegisterRequest;
import com.minitrello.application.shared.ApiResponse;
import com.minitrello.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    @Operation(summary = "Register a new user account")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        AuthService.AuthResult result = authService.register(request);
        return respondWithTokens(result, httpRequest, "Account created successfully");
    }

    @Operation(summary = "Log in with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthService.AuthResult result = authService.login(request);
        return respondWithTokens(result, httpRequest, "Logged in successfully");
    }

    @Operation(summary = "Exchange a valid refresh token (httpOnly cookie) for a new access token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        if (refreshToken == null) {
            throw new com.minitrello.domain.shared.exception.ForbiddenOperationException("No refresh token provided");
        }
        AuthService.AuthResult result = authService.refresh(refreshToken);
        return respondWithTokens(result, httpRequest, "Token refreshed successfully");
    }

    @Operation(summary = "Log out — revokes all refresh tokens for the current user")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserPrincipal principal, HttpServletRequest httpRequest) {
        authService.logout(principal.getId());

        ResponseCookie clearCookie = buildRefreshCookie("", Duration.ZERO);
        return ResponseEntity.ok()
                .header("Set-Cookie", clearCookie.toString())
                .body(ApiResponse.success(null, "Logged out successfully", httpRequest.getRequestURI()));
    }

    private ResponseEntity<ApiResponse<AuthResponse>> respondWithTokens(
            AuthService.AuthResult result, HttpServletRequest httpRequest, String message) {
        ResponseCookie cookie = buildRefreshCookie(result.rawRefreshToken(), Duration.ofDays(refreshTokenExpirationDays));
        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(ApiResponse.success(result.response(), message, httpRequest.getRequestURI()));
    }

    private ResponseCookie buildRefreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
