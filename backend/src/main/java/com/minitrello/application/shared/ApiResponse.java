package com.minitrello.application.shared;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard success/error envelope for every REST response, per the API
 * standard defined in the project spec. Built via static factories rather
 * than a public constructor so callers can't accidentally construct an
 * inconsistent state (e.g. success=true with a populated errors list).
 *
 * `path` is filled in by GlobalExceptionHandler / a response advice rather
 * than by each controller, so controllers never have to think about it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        List<ApiError> errors,
        Instant timestamp,
        String path
) {

    public static <T> ApiResponse<T> success(T data, String message, String path) {
        return new ApiResponse<>(true, message, data, null, Instant.now(), path);
    }

    public static <T> ApiResponse<T> success(T data, String path) {
        return success(data, "Request completed successfully", path);
    }

    public static ApiResponse<Void> error(String message, List<ApiError> errors, String path) {
        return new ApiResponse<>(false, message, null, errors, Instant.now(), path);
    }
}
