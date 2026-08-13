package com.minitrello.domain.shared.exception;

/**
 * Thrown when an authenticated user is known, but lacks permission for
 * the specific resource/action (resource-level authorization — see
 * Phase 2 authorization flow, e.g. a CONTRIBUTOR trying to delete a
 * project).
 *
 * Maps to HTTP 403 in GlobalExceptionHandler.
 * Distinct from Spring Security's AccessDeniedException (which we also
 * catch) to keep domain-level authorization decisions framework-free.
 */
public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
