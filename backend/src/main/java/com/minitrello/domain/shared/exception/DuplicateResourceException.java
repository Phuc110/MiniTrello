package com.minitrello.domain.shared.exception;

/**
 * Thrown on uniqueness-constraint violations enforced at the application
 * level before hitting the database (e.g. duplicate workspace slug,
 * duplicate email on registration) — checked proactively so we can
 * return a clean field-level error instead of surfacing a raw SQL
 * constraint violation to the client.
 *
 * Maps to HTTP 409 (Conflict) in GlobalExceptionHandler.
 */
public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
