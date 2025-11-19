package com.telecom.ocs.provisioning.exceptions;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Typically results in HTTP 409 Conflict response.
 */
public class DuplicateResourceException extends RuntimeException {

    private final String resourceType;
    private final String conflictingField;
    private final String conflictingValue;

    /**
     * Constructs a new DuplicateResourceException with the specified details.
     *
     * @param resourceType the type of resource that already exists (e.g., "Subscriber", "Group")
     * @param conflictingField the field that has the duplicate value (e.g., "msisdn", "groupName")
     * @param conflictingValue the duplicate value
     */
    public DuplicateResourceException(String resourceType, String conflictingField, String conflictingValue) {
        super(String.format("%s with %s '%s' already exists", resourceType, conflictingField, conflictingValue));
        this.resourceType = resourceType;
        this.conflictingField = conflictingField;
        this.conflictingValue = conflictingValue;
    }

    /**
     * Constructs a new DuplicateResourceException with a custom message.
     *
     * @param message the detail message
     */
    public DuplicateResourceException(String message) {
        super(message);
        this.resourceType = null;
        this.conflictingField = null;
        this.conflictingValue = null;
    }

    /**
     * Constructs a new DuplicateResourceException with the specified details and cause.
     *
     * @param resourceType the type of resource that already exists
     * @param conflictingField the field that has the duplicate value
     * @param conflictingValue the duplicate value
     * @param cause the cause of the exception
     */
    public DuplicateResourceException(String resourceType, String conflictingField, String conflictingValue, Throwable cause) {
        super(String.format("%s with %s '%s' already exists", resourceType, conflictingField, conflictingValue), cause);
        this.resourceType = resourceType;
        this.conflictingField = conflictingField;
        this.conflictingValue = conflictingValue;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getConflictingField() {
        return conflictingField;
    }

    public String getConflictingValue() {
        return conflictingValue;
    }
}
