package com.nira.finance.exception;

/** Thrown when a lookup by id fails - maps to a 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
