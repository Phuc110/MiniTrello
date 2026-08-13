package com.minitrello.presentation.controller;

import com.minitrello.application.notification.NotificationResponse;
import com.minitrello.application.notification.NotificationService;
import com.minitrello.application.shared.ApiResponse;
import com.minitrello.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get all notifications for the authenticated user")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listAll(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        List<NotificationResponse> notifications = notificationService.listAll(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(notifications, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Get unread notifications for the authenticated user")
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listUnread(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        List<NotificationResponse> unread = notificationService.listUnread(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(unread, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Mark a single notification as read")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        NotificationResponse updated = notificationService.markRead(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(updated, "Notification marked as read", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Mark all notifications as read for the authenticated user")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        notificationService.markAllRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read", httpRequest.getRequestURI()));
    }
}
