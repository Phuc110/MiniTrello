package com.minitrello.application.shared;

/**
 * A single field-level or general error entry inside ApiResponse.errors.
 * `field` is null for errors that aren't tied to a specific request field
 * (e.g. "project not found" vs. a validation error on "email").
 */
public record ApiError(String field, String message) {

    public static ApiError of(String message) {
        return new ApiError(null, message);
    }

    public static ApiError of(String field, String message) {
        return new ApiError(field, message);
    }
}
