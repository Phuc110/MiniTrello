package com.minitrello.domain.shared.exception;

import java.util.UUID;

/**
 * Thrown when a requested entity does not exist (or is soft-deleted,
 * which — thanks to @SQLRestriction — looks identical to "does not exist"
 * at the query level, which is intentional: we never reveal that a
 * soft-deleted record exists to a caller without delete permissions).
 *
 * Maps to HTTP 404 in GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String entityName, UUID id) {
        super("%s with id [%s] was not found".formatted(entityName, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
