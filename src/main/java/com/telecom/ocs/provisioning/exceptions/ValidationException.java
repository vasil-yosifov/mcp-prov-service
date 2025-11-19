package com.telecom.ocs.provisioning.exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when request validation fails.
 * Typically results in HTTP 422 Unprocessable Entity response.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    /**
     * Constructs a new ValidationException with the specified message.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = new HashMap<>();
    }

    /**
     * Constructs a new ValidationException with field-level error details.
     *
     * @param message the detail message
     * @param fieldErrors map of field names to error messages
     */
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? new HashMap<>(fieldErrors) : new HashMap<>();
    }

    /**
     * Constructs a new ValidationException with a single field error.
     *
     * @param message the detail message
     * @param fieldName the name of the invalid field
     * @param fieldError the error message for the field
     */
    public ValidationException(String message, String fieldName, String fieldError) {
        super(message);
        this.fieldErrors = new HashMap<>();
        this.fieldErrors.put(fieldName, fieldError);
    }

    /**
     * Constructs a new ValidationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.fieldErrors = new HashMap<>();
    }

    public Map<String, String> getFieldErrors() {
        return new HashMap<>(fieldErrors);
    }

    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}
