package com.nira.finance.exception;

/** Thrown when a user tries to act on a resource that isn't theirs - maps to a 403. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
