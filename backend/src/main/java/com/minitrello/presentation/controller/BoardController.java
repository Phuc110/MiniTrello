package com.minitrello.presentation.controller;

import com.minitrello.application.board.BoardService;
import com.minitrello.application.board.dto.BoardResponse;
import com.minitrello.application.board.dto.CreateBoardRequest;
import com.minitrello.application.shared.ApiResponse;
import com.minitrello.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Boards")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "Create a board within a project — caller must be a project member")
    @PostMapping("/api/projects/{projectId}/boards")
    public ResponseEntity<ApiResponse<BoardResponse>> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateBoardRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        BoardResponse response = boardService.createBoard(projectId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Board created successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "List all boards in a project")
    @GetMapping("/api/projects/{projectId}/boards")
    public ResponseEntity<ApiResponse<List<BoardResponse>>> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        List<BoardResponse> response = boardService.listBoards(projectId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }
}
