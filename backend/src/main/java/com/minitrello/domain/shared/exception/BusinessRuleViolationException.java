package com.minitrello.domain.shared.exception;

/**
 * Thrown when an operation is structurally valid (well-formed request,
 * authorized caller) but violates a business invariant — e.g. inviting
 * a user who is already a project member, or deleting a workspace that
 * still has active projects.
 *
 * Maps to HTTP 409 (Conflict) in GlobalExceptionHandler.
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
