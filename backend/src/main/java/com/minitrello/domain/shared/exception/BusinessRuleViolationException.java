package com.minitrello.domain.shared.exception;

/**
 * Thrown when an operation is structurally valid (well-formed request,
 * authorized caller) but violates a business invariant — e.g. inviting
 * a user who is already a workspace member, or moving a task to a
 * list in a different workspace.
 *
 * Maps to HTTP 409 (Conflict) in GlobalExceptionHandler.
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
