package com.telecom.ocs.provisioning.exceptions;

/**
 * Exception thrown when an optimistic locking conflict occurs during concurrent updates.
 * This happens when the resource's lastModifiedDate or version has changed since it was retrieved.
 * Typically results in HTTP 409 Conflict response.
 * 
 * Implements FR-072: Concurrent update handling via optimistic locking.
 */
public class OptimisticLockingException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;
    private final Long expectedVersion;
    private final Long actualVersion;

    /**
     * Constructs a new OptimisticLockingException with the specified resource details.
     *
     * @param resourceType the type of resource that had a locking conflict (e.g., "Subscriber", "Subscription")
     * @param resourceId the unique identifier of the resource
     */
    public OptimisticLockingException(String resourceType, String resourceId) {
        super(String.format("Concurrent modification detected for %s with ID '%s'. Resource was modified by another request.", 
            resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.expectedVersion = null;
        this.actualVersion = null;
    }

    /**
     * Constructs a new OptimisticLockingException with version details.
     *
     * @param resourceType the type of resource that had a locking conflict
     * @param resourceId the unique identifier of the resource
     * @param expectedVersion the version expected by the client
     * @param actualVersion the current version in the database
     */
    public OptimisticLockingException(String resourceType, String resourceId, Long expectedVersion, Long actualVersion) {
        super(String.format("Concurrent modification detected for %s with ID '%s'. Expected version %d but found version %d.", 
            resourceType, resourceId, expectedVersion, actualVersion));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    /**
     * Constructs a new OptimisticLockingException with a custom message.
     *
     * @param message the detail message
     */
    public OptimisticLockingException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
        this.expectedVersion = null;
        this.actualVersion = null;
    }

    /**
     * Constructs a new OptimisticLockingException with the specified resource details and cause.
     *
     * @param resourceType the type of resource that had a locking conflict
     * @param resourceId the unique identifier of the resource
     * @param cause the cause of the exception (typically JPA OptimisticLockException)
     */
    public OptimisticLockingException(String resourceType, String resourceId, Throwable cause) {
        super(String.format("Concurrent modification detected for %s with ID '%s'", resourceType, resourceId), cause);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.expectedVersion = null;
        this.actualVersion = null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public Long getActualVersion() {
        return actualVersion;
    }
}
