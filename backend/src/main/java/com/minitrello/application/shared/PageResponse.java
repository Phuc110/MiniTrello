package com.minitrello.application.shared;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wraps Spring Data's Page<T> into a stable, framework-agnostic shape for
 * API responses. We never serialize Spring's Page directly — its JSON
 * shape has changed across versions before, and leaking it couples the
 * public API contract to an implementation detail of Spring Data.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
