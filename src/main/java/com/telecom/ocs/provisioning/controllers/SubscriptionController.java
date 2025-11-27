package com.telecom.ocs.provisioning.controllers;

import com.telecom.ocs.provisioning.api.model.PatchOperation;
import com.telecom.ocs.provisioning.mappers.SubscriptionMapper;
import com.telecom.ocs.provisioning.models.Subscription;
import com.telecom.ocs.provisioning.services.SubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller implementing subscription-related endpoints.
 * 
 * Implements operations for:
 * - Creating subscriptions for subscribers (POST /subscribers/{subscriberId}/subscriptions)
 * - Creating subscriptions directly (POST /subscriptions)
 * - Listing subscriptions for a subscriber (GET /subscribers/{subscriberId}/subscriptions)
 * - Getting subscription by ID (GET /subscriptions/{subscriptionId})
 * - Partially updating subscription (PATCH /subscriptions/{subscriptionId})
 * - Deleting subscription (DELETE /subscriptions/{subscriptionId})
 * 
 * Uses SubscriptionService for business logic and SubscriptionMapper for DTO conversion.
 * 
 * Supports task T067 (SubscriptionController implementation)
 * 
 * @see SubscriptionService
 * @see SubscriptionMapper
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionMapper subscriptionMapper;

    // =========================================================================
    // Subscriber-scoped Subscription Endpoints
    // =========================================================================

    /**
     * POST /subscribers/{subscriberId}/subscriptions : Create a subscription for subscriber
     * 
     * Creates a new subscription for the specified subscriber. The service layer validates
     * subscriber existence and sets default state to PENDING.
     *
     * @param subscriberId Subscriber UUID (path variable)
     * @param subscriptionDto Subscription DTO from request body
     * @return Subscription created (status code 201)
     *         or Conflict (status code 409) if subscriptionId already exists
     *         or Resource not found (status code 404) if subscriber not found
     */
    @PostMapping(value = "/subscribers/{subscriberId}/subscriptions", 
                 produces = "application/json", 
                 consumes = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscription> subscribersSubscriberIdSubscriptionsPost(
            @PathVariable("subscriberId") String subscriberId,
            @RequestBody com.telecom.ocs.provisioning.api.model.Subscription subscriptionDto) {
        
        log.info("Received request to create subscription for subscriber: {} with offerId: {}", 
                subscriberId, subscriptionDto.getOfferId());
        
        // Convert DTO to entity
        Subscription entity = subscriptionMapper.toEntity(subscriptionDto);
        
        // Create subscription (service validates subscriber existence and sets defaults)
        Subscription created = subscriptionService.createSubscription(subscriberId, entity);
        
        // Convert back to DTO
        com.telecom.ocs.provisioning.api.model.Subscription responseDto = subscriptionMapper.toDto(created);
        
        log.info("Successfully created subscription with ID: {} for subscriber: {}", 
                created.getSubscriptionId(), subscriberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * GET /subscribers/{subscriberId}/subscriptions : List subscriptions for a subscriber
     * 
     * Retrieves all subscriptions associated with the specified subscriber.
     *
     * @param subscriberId Subscriber UUID (path variable)
     * @return List of subscriptions (status code 200)
     *         or Resource not found (status code 404) if subscriber not found
     */
    @GetMapping(value = "/subscribers/{subscriberId}/subscriptions", 
                produces = "application/json")
    public ResponseEntity<List<com.telecom.ocs.provisioning.api.model.Subscription>> subscribersSubscriberIdSubscriptionsGet(
            @PathVariable("subscriberId") String subscriberId) {
        
        log.info("Received request to list subscriptions for subscriber: {}", subscriberId);
        
        // Get subscriptions (service validates subscriber existence)
        List<Subscription> subscriptions = subscriptionService.getSubscriptionsBySubscriberId(subscriberId);
        
        // Convert to DTOs
        List<com.telecom.ocs.provisioning.api.model.Subscription> responseDtos = 
                subscriptions.stream()
                        .map(subscriptionMapper::toDto)
                        .collect(Collectors.toList());
        
        log.info("Successfully retrieved {} subscriptions for subscriber: {}", 
                responseDtos.size(), subscriberId);
        return ResponseEntity.ok(responseDtos);
    }

    // =========================================================================
    // Direct Subscription Endpoints
    // =========================================================================

    /**
     * POST /subscriptions : Create subscription directly
     * 
     * Creates a new subscription. The subscriberId must be provided in the request body.
     * The service layer validates subscriber existence and sets default state to PENDING.
     *
     * @param subscriptionDto Subscription DTO from request body (must include subscriberId)
     * @return Subscription created (status code 201)
     *         or Conflict (status code 409) if subscriptionId already exists
     *         or Bad request (status code 400) if subscriberId is missing
     */
    @PostMapping(value = "/subscriptions", 
                 produces = "application/json", 
                 consumes = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscription> subscriptionsPost(
            @RequestBody com.telecom.ocs.provisioning.api.model.Subscription subscriptionDto) {
        
        String subscriberId = subscriptionDto.getSubscriberId();
        
        log.info("Received request to create subscription with offerId: {} for subscriber: {}", 
                subscriptionDto.getOfferId(), subscriberId);
        
        // Validate subscriberId is provided
        if (subscriberId == null || subscriberId.isBlank()) {
            log.warn("subscriberId is required in request body");
            return ResponseEntity.badRequest().build();
        }
        
        // Convert DTO to entity
        Subscription entity = subscriptionMapper.toEntity(subscriptionDto);
        
        // Create subscription (service validates subscriber existence and sets defaults)
        Subscription created = subscriptionService.createSubscription(subscriberId, entity);
        
        // Convert back to DTO
        com.telecom.ocs.provisioning.api.model.Subscription responseDto = subscriptionMapper.toDto(created);
        
        log.info("Successfully created subscription with ID: {}", created.getSubscriptionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * GET /subscriptions/{subscriptionId} : Get subscription by ID
     * 
     * Retrieves a subscription by its unique ID.
     *
     * @param subscriptionId Subscription UUID
     * @return Subscription found (status code 200)
     *         or Resource not found (status code 404)
     */
    @GetMapping(value = "/subscriptions/{subscriptionId}", 
                produces = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscription> subscriptionsSubscriptionIdGet(
            @PathVariable("subscriptionId") String subscriptionId) {
        
        log.info("Received request to get subscription by ID: {}", subscriptionId);
        
        // Service throws ResourceNotFoundException if not found
        Subscription entity = subscriptionService.getSubscriptionById(subscriptionId);
        
        // Convert to DTO
        com.telecom.ocs.provisioning.api.model.Subscription responseDto = subscriptionMapper.toDto(entity);
        
        log.info("Successfully retrieved subscription with ID: {}", subscriptionId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * PATCH /subscriptions/{subscriptionId} : Update subscription partially
     * 
     * Supports JSON Patch operations for updating specific fields.
     * State transitions should be handled through specific operations in the DTO
     * or dedicated state management endpoints.
     *
     * @param subscriptionId Subscription UUID
     * @param patchOperations List of patch operations (fieldName, fieldValue)
     * @return Subscription patched (status code 200)
     *         or Bad request (status code 400)
     *         or Resource not found (status code 404)
     *         or Field to patch not found (status code 422)
     */
    @PatchMapping(value = "/subscriptions/{subscriptionId}", 
                  produces = "application/json", 
                  consumes = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscription> subscriptionsSubscriptionIdPatch(
            @PathVariable("subscriptionId") String subscriptionId,
            @Valid @Size(min = 1) @RequestBody List<PatchOperation> patchOperations) {
        
        log.info("Received PATCH request for subscription ID: {} with {} operations", 
                subscriptionId, patchOperations.size());
        
        try {
            // Get existing subscription
            Subscription existing = subscriptionService.getSubscriptionById(subscriptionId);
            
            // Apply patch operations
            applyPatchOperations(existing, patchOperations);
            
            // Update subscription
            Subscription updated = subscriptionService.updateSubscription(subscriptionId, existing);
            
            // Convert to DTO
            com.telecom.ocs.provisioning.api.model.Subscription responseDto = subscriptionMapper.toDto(updated);
            
            log.info("Successfully patched subscription with ID: {}", subscriptionId);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            log.error("Invalid patch operation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    /**
     * DELETE /subscriptions/{subscriptionId} : Delete subscription
     * 
     * Deletes a subscription and all associated data (cascade delete).
     * This is a hard delete operation.
     *
     * @param subscriptionId Subscription UUID
     * @return Subscription deleted (status code 204)
     */
    @DeleteMapping(value = "/subscriptions/{subscriptionId}")
    public ResponseEntity<Void> subscriptionsSubscriptionIdDelete(
            @PathVariable("subscriptionId") String subscriptionId) {
        
        log.info("Received request to delete subscription with ID: {}", subscriptionId);
        
        // Delete subscription (service throws ResourceNotFoundException if not found)
        subscriptionService.deleteSubscription(subscriptionId);
        
        log.info("Successfully deleted subscription with ID: {}", subscriptionId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // State Transition Endpoints
    // =========================================================================

    /**
     * POST /subscriptions/{subscriptionId}/activate : Activate a subscription
     * 
     * Transitions subscription from PENDING or SUSPENDED to ACTIVE state.
     * Sets activationDate and calculates renewalDate for recurring subscriptions (FR-017).
     *
     * @param subscriptionId Subscription UUID
     * @return Activated subscription (status code 200)
     *         or Resource not found (status code 404)
     *         or Bad request (status code 400) if invalid state transition
     */
    @PostMapping(value = "/subscriptions/{subscriptionId}/activate", 
                 produces = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscription> activateSubscription(
            @PathVariable("subscriptionId") String subscriptionId) {
        
        log.info("Received request to activate subscription: {}", subscriptionId);
        
        try {
            Subscription activated = subscriptionService.activateSubscription(subscriptionId);
            com.telecom.ocs.provisioning.api.model.Subscription responseDto = subscriptionMapper.toDto(activated);
            
            log.info("Successfully activated subscription: {}", subscriptionId);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalStateException e) {
            log.warn("Invalid state transition for activate: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /subscriptions/{subscriptionId}/suspend : Suspend a subscription
     * 
     * Transitions subscription from ACTIVE to SUSPENDED state.
     *
     * @param subscriptionId Subscription UUID
     * @return Suspended subscription (status code 200)
     *         or Resource not found (status code 404)
     *         or Bad request (status code 400) if invalid state transition
     */
    @PostMapping(value = "/subscriptions/{subscriptionId}/suspend", 
                 produces = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscription> suspendSubscription(
            @PathVariable("subscriptionId") String subscriptionId) {
        
        log.info("Received request to suspend subscription: {}", subscriptionId);
        
        try {
            Subscription suspended = subscriptionService.suspendSubscription(subscriptionId);
            com.telecom.ocs.provisioning.api.model.Subscription responseDto = subscriptionMapper.toDto(suspended);
            
            log.info("Successfully suspended subscription: {}", subscriptionId);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalStateException e) {
            log.warn("Invalid state transition for suspend: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /subscriptions/{subscriptionId}/cancel : Cancel a subscription
     * 
     * Transitions subscription from ACTIVE or SUSPENDED to CANCELLED state.
     *
     * @param subscriptionId Subscription UUID
     * @return Cancelled subscription (status code 200)
     *         or Resource not found (status code 404)
     *         or Bad request (status code 400) if invalid state transition
     */
    @PostMapping(value = "/subscriptions/{subscriptionId}/cancel", 
                 produces = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscription> cancelSubscription(
            @PathVariable("subscriptionId") String subscriptionId) {
        
        log.info("Received request to cancel subscription: {}", subscriptionId);
        
        try {
            Subscription cancelled = subscriptionService.cancelSubscription(subscriptionId);
            com.telecom.ocs.provisioning.api.model.Subscription responseDto = subscriptionMapper.toDto(cancelled);
            
            log.info("Successfully cancelled subscription: {}", subscriptionId);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalStateException e) {
            log.warn("Invalid state transition for cancel: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /subscriptions/{subscriptionId}/renew : Process renewal cycle
     * 
     * Increments recurring cycle count for the subscription.
     * May auto-transition to EXPIRED if max cycles reached (FR-015).
     *
     * @param subscriptionId Subscription UUID
     * @return Updated subscription (status code 200)
     *         or Resource not found (status code 404)
     *         or Bad request (status code 400) if subscription is not active
     */
    @PostMapping(value = "/subscriptions/{subscriptionId}/renew", 
                 produces = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscription> renewSubscription(
            @PathVariable("subscriptionId") String subscriptionId) {
        
        log.info("Received request to renew subscription: {}", subscriptionId);
        
        try {
            Subscription renewed = subscriptionService.incrementRecurringCycle(subscriptionId);
            com.telecom.ocs.provisioning.api.model.Subscription responseDto = subscriptionMapper.toDto(renewed);
            
            log.info("Successfully renewed subscription: {} (state: {})", 
                    subscriptionId, renewed.getState());
            return ResponseEntity.ok(responseDto);
        } catch (IllegalStateException e) {
            log.warn("Cannot renew subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Apply patch operations to subscription entity
     * 
     * @param subscription the subscription entity to update
     * @param patchOperations list of patch operations
     * @throws IllegalArgumentException if an unsupported field is encountered
     */
    private void applyPatchOperations(Subscription subscription, List<PatchOperation> patchOperations) {
        for (PatchOperation op : patchOperations) {
            String fieldName = op.getFieldName();
            // Unwrap JsonNullable to get the actual value
            JsonNullable<Object> jsonNullableValue = op.getFieldValue();
            Object fieldValue = jsonNullableValue != null && jsonNullableValue.isPresent() 
                    ? jsonNullableValue.get() : null;
            
            log.debug("Applying patch operation: {} = {}", fieldName, fieldValue);
            
            if (fieldValue == null) {
                log.warn("Null value for patch field: {}", fieldName);
                continue;
            }
            
            switch (fieldName) {
                case "offerName" -> subscription.setOfferName(fieldValue.toString());
                case "subscriptionType" -> subscription.setSubscriptionType(fieldValue.toString());
                case "recurring" -> subscription.setRecurring(convertToBoolean(fieldValue));
                case "paidFlag" -> subscription.setPaidFlag(convertToBoolean(fieldValue));
                case "isGroup" -> subscription.setIsGroup(convertToBoolean(fieldValue));
                case "maxRecurringCycles" -> subscription.setMaxRecurringCycles(convertToInteger(fieldValue));
                case "cycleLengthUnits" -> subscription.setCycleLengthUnits(convertToInteger(fieldValue));
                case "cycleLengthType" -> subscription.setCycleLengthType(
                        Subscription.CycleLengthType.valueOf(fieldValue.toString().toUpperCase()));
                case "customParameters" -> subscription.setCustomParameters(fieldValue.toString());
                default -> {
                    log.warn("Unsupported patch field: {}", fieldName);
                    throw new IllegalArgumentException("Unsupported patch field: " + fieldName);
                }
            }
        }
    }

    /**
     * Convert value to Boolean
     */
    private Boolean convertToBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Convert value to Integer
     */
    private Integer convertToInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
