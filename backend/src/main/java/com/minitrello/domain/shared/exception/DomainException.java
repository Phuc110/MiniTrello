package com.minitrello.domain.shared.exception;

/**
 * Root of the domain exception hierarchy.
 *
 * These live in the domain layer (not infrastructure) so business rules
 * can throw them without depending on Spring or HTTP. The presentation
 * layer's GlobalExceptionHandler is the only place that knows how to
 * translate them into HTTP status codes — the domain never imports
 * anything HTTP-related.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
