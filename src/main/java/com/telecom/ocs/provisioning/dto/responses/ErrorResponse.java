package com.telecom.ocs.provisioning.dto.responses;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Structured error response DTO for REST API error handling.
 * Used by GlobalExceptionHandler to provide consistent error responses.
 * Implements Constitution Principle VI: Data Validation and Error Handling.
 */
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> details;

    public ErrorResponse() {
        this.details = new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void addDetail(String key, String value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
    }

    // Getters and setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    /**
     * Builder for ErrorResponse.
     */
    public static class Builder {
        private final ErrorResponse errorResponse;

        public Builder() {
            this.errorResponse = new ErrorResponse();
        }

        public Builder timestamp(LocalDateTime timestamp) {
            errorResponse.timestamp = timestamp;
            return this;
        }

        public Builder status(int status) {
            errorResponse.status = status;
            return this;
        }

        public Builder error(String error) {
            errorResponse.error = error;
            return this;
        }

        public Builder message(String message) {
            errorResponse.message = message;
            return this;
        }

        public Builder path(String path) {
            errorResponse.path = path;
            return this;
        }

        public Builder details(Map<String, String> details) {
            errorResponse.details = details != null ? new HashMap<>(details) : new HashMap<>();
            return this;
        }

        public ErrorResponse build() {
            return errorResponse;
        }
    }
}
