package com.minitrello.presentation.controller;

import com.minitrello.application.board.BoardListService;
import com.minitrello.application.board.dto.BoardListResponse;
import com.minitrello.application.board.dto.CreateBoardListRequest;
import com.minitrello.application.board.dto.MoveBoardListRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Board Lists")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class BoardListController {

    private final BoardListService boardListService;

    @Operation(summary = "Create a Kanban column on a board — appended at the end by default")
    @PostMapping("/api/boards/{boardId}/lists")
    public ResponseEntity<ApiResponse<BoardListResponse>> create(
            @PathVariable UUID boardId,
            @Valid @RequestBody CreateBoardListRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        BoardListResponse response = boardListService.createList(boardId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "List created successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "List all columns on a board, in display order")
    @GetMapping("/api/boards/{boardId}/lists")
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> list(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        List<BoardListResponse> response = boardListService.listForBoard(boardId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response, httpRequest.getRequestURI()));
    }

    @Operation(summary = "Reorder a column via drag-and-drop — provide the desired new neighbors, not a raw position")
    @PatchMapping("/api/board-lists/{boardListId}/position")
    public ResponseEntity<ApiResponse<BoardListResponse>> move(
            @PathVariable UUID boardListId,
            @RequestBody MoveBoardListRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        BoardListResponse response = boardListService.moveList(boardListId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "List reordered successfully", httpRequest.getRequestURI()));
    }

    @Operation(summary = "Soft-delete a list and all its tasks")
    @DeleteMapping("/api/board-lists/{boardListId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID boardListId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            HttpServletRequest httpRequest) {
        boardListService.deleteList(boardListId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "List deleted successfully", httpRequest.getRequestURI()));
    }
}
