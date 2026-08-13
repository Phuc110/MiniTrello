package com.minitrello.presentation.advice;

import com.minitrello.application.shared.ApiError;
import com.minitrello.application.shared.ApiResponse;
import com.minitrello.domain.shared.exception.BusinessRuleViolationException;
import com.minitrello.domain.shared.exception.DuplicateResourceException;
import com.minitrello.domain.shared.exception.ForbiddenOperationException;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Single place where every exception in the system is translated into the
 * standard ApiResponse error envelope. Controllers and services never
 * build error responses themselves — they just throw the appropriate
 * domain exception and let this class decide the HTTP status.
 *
 * Ordering matters: more specific handlers are listed before generic ones,
 * though Spring resolves by exception type specificity regardless of
 * declaration order.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(ApiError.of(ex.getMessage())), request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(
            ForbiddenOperationException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), List.of(ApiError.of(ex.getMessage())), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSpringAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action",
                List.of(ApiError.of("Access denied")), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password",
                List.of(ApiError.of("Invalid credentials")), request);
    }

    @ExceptionHandler({BusinessRuleViolationException.class, DuplicateResourceException.class})
    public ResponseEntity<ApiResponse<Void>> handleConflict(
            RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of(ApiError.of(ex.getMessage())), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toApiError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        // Log full detail server-side; never leak stack traces / internal
        // messages to the client — that's an information-disclosure risk.
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                List.of(ApiError.of("Internal server error")), request);
    }

    private ApiError toApiError(FieldError fieldError) {
        return ApiError.of(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiResponse<Void>> build(
            HttpStatus status, String message, List<ApiError> errors, HttpServletRequest request) {
        ApiResponse<Void> body = ApiResponse.error(message, errors, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
