package com.telecom.ocs.provisioning.exceptions;

/**
 * Exception thrown when a requested resource is not found in the system.
 * Typically results in HTTP 404 Not Found response.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    /**
     * Constructs a new ResourceNotFoundException with the specified resource type and ID.
     *
     * @param resourceType the type of resource that was not found (e.g., "Subscriber", "Subscription")
     * @param resourceId the unique identifier of the resource
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with ID '%s' not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Constructs a new ResourceNotFoundException with a custom message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified resource type, ID, and cause.
     *
     * @param resourceType the type of resource that was not found
     * @param resourceId the unique identifier of the resource
     * @param cause the cause of the exception
     */
    public ResourceNotFoundException(String resourceType, String resourceId, Throwable cause) {
        super(String.format("%s with ID '%s' not found", resourceType, resourceId), cause);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
