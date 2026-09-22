package com.nira.finance.exception;

import java.time.Instant;
import java.util.List;

/** Uniform error shape returned by every endpoint, so the frontend never has to guess. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(Instant.now(), status, error, message, List.of());
    }

    public static ErrorResponse ofFieldErrors(int status, String error, List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, "Validation failed", fieldErrors);
    }
}
