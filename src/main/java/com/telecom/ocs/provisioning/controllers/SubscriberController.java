package com.telecom.ocs.provisioning.controllers;

import com.telecom.ocs.provisioning.api.model.PatchOperation;
import com.telecom.ocs.provisioning.api.model.SubscriberIdResponse;
import com.telecom.ocs.provisioning.mappers.SubscriberMapper;
import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.services.SubscriberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller implementing subscriber-related endpoints.
 * 
 * Implements operations for:
 * - Creating subscribers (POST /subscribers/)
 * - Getting subscriber by ID (GET /subscribers/{subscriberId})
 * - Lookup by msisdn, imsi, or name (GET /subscribers/lookup)
 * - Partially updating subscriber (PATCH /subscribers/{subscriberId})
 * - Deleting subscriber (DELETE /subscribers/{subscriberId})
 * 
 * Uses SubscriberService for business logic and SubscriberMapper for DTO conversion.
 * 
 * @see SubscriberService
 * @see SubscriberMapper
 */
@RestController
@RequestMapping("/subscribers")
@RequiredArgsConstructor
@Slf4j
public class SubscriberController {

    private final SubscriberService subscriberService;
    private final SubscriberMapper subscriberMapper;

    /**
     * POST /subscribers/ : Create a subscriber
     * 
     * Creates a new subscriber. The service layer validates msisdn uniqueness
     * (FR-007) and sets default state to PRE_PROVISIONED.
     *
     * @param subscriberDto Subscriber DTO from request body
     * @return Subscriber created (status code 201)
     *         or Conflict (status code 409) if msisdn already exists
     */
    @PostMapping(produces = "application/json", consumes = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscriber> subscribersPost(
            @RequestBody com.telecom.ocs.provisioning.api.model.Subscriber subscriberDto) {
        
        log.info("Received request to create subscriber with msisdn: {}", 
                subscriberDto.getMsisdn() != null ? subscriberDto.getMsisdn() : "null");
        
        log.info("Received request to create subscriber : {}", subscriberDto);
        // Convert DTO to entity
        Subscriber entity = subscriberMapper.toEntity(subscriberDto);
        
        // Create subscriber (service validates uniqueness and sets defaults)
        Subscriber created = subscriberService.createSubscriber(entity);
        
        // Convert back to DTO
        com.telecom.ocs.provisioning.api.model.Subscriber responseDto = subscriberMapper.toDto(created);
        
        log.info("Successfully created subscriber with ID: {}", created.getSubscriberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * GET /subscribers/{subscriberId} : Get subscriber by ID
     * 
     * Retrieves a subscriber by their unique ID.
     *
     * @param subscriberId Subscriber UUID
     * @return Subscriber found (status code 200)
     *         or Resource not found (status code 404)
     */
    @GetMapping(value = "/{subscriberId}", produces = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscriber> subscribersSubscriberIdGet(
            @PathVariable("subscriberId") String subscriberId) {
        
        log.info("Received request to get subscriber by ID: {}", subscriberId);
        
        // Service throws ResourceNotFoundException if not found
        Subscriber entity = subscriberService.getSubscriberById(subscriberId);
        
        // Convert to DTO
        com.telecom.ocs.provisioning.api.model.Subscriber responseDto = subscriberMapper.toDto(entity);
        
        log.info("Successfully retrieved subscriber with ID: {}", subscriberId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * GET /subscribers/lookup : Lookup subscriberId by msisdn, imsi or first and last name
     * 
     * Supports three lookup strategies:
     * 1. By msisdn (phone number)
     * 2. By imsi (SIM card ID)
     * 3. By firstName AND lastName (both required)
     * 
     * Only one strategy should be used per request. Priority: msisdn > imsi > name.
     *
     * @param msisdn MSISDN number to lookup (optional)
     * @param imsi IMSI to lookup (optional)
     * @param firstName Subscriber first name to lookup (requires lastName) (optional)
     * @param lastName Subscriber last name to lookup (requires firstName) (optional)
     * @return SubscriberId found (status code 200)
     *         or Bad request (status code 400) if multiple strategies used or name incomplete
     *         or Resource not found (status code 404) if no subscriber found
     */
    @GetMapping(value = "/lookup", produces = "application/json")
    public ResponseEntity<SubscriberIdResponse> subscribersLookupGet(
            @Valid @RequestParam(value = "msisdn", required = false) String msisdn,
            @Valid @RequestParam(value = "imsi", required = false) String imsi,
            @Valid @RequestParam(value = "firstName", required = false) String firstName,
            @Valid @RequestParam(value = "lastName", required = false) String lastName) {
        
        log.info("Received lookup request - msisdn: {}, imsi: {}, firstName: {}, lastName: {}", 
                msisdn, imsi, firstName, lastName);
        
        Optional<Subscriber> result = Optional.empty();
        
        // Strategy 1: Lookup by msisdn (highest priority)
        if (msisdn != null && !msisdn.isBlank()) {
            log.debug("Looking up subscriber by msisdn: {}", msisdn);
            result = subscriberService.lookupByMsisdn(msisdn);
        }
        // Strategy 2: Lookup by imsi
        else if (imsi != null && !imsi.isBlank()) {
            log.debug("Looking up subscriber by imsi: {}", imsi);
            result = subscriberService.lookupByImsi(imsi);
        }
        // Strategy 3: Lookup by name (requires both firstName AND lastName)
        else if ((firstName != null && !firstName.isBlank()) && (lastName != null && !lastName.isBlank())) {
            log.debug("Looking up subscriber by name: {} {}", firstName, lastName);
            List<Subscriber> subscribers = subscriberService.lookupByName(firstName, lastName);
            result = subscribers.isEmpty() ? Optional.empty() : Optional.of(subscribers.get(0));
            
            if (subscribers.size() > 1) {
                log.warn("Multiple subscribers found for name {} {} - returning first match", firstName, lastName);
            }
        }
        // Incomplete name query
        else if ((firstName != null && !firstName.isBlank()) || (lastName != null && !lastName.isBlank())) {
            log.warn("Incomplete name lookup - both firstName and lastName are required");
            return ResponseEntity.badRequest().build();
        }
        // No valid lookup strategy
        else {
            log.warn("No valid lookup strategy provided");
            return ResponseEntity.badRequest().build();
        }
        
        // Check if subscriber was found
        if (result.isEmpty()) {
            log.info("No subscriber found for lookup criteria");
            return ResponseEntity.notFound().build();
        }
        
        // Build response with subscriberId
        SubscriberIdResponse response = new SubscriberIdResponse();
        response.setSubscriberId(result.get().getSubscriberId());
        
        log.info("Successfully looked up subscriber with ID: {}", response.getSubscriberId());
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /subscribers/{subscriberId} : Update subscriber partially
     * 
     * Supports JSON Patch operations for updating specific fields.
     * Uses dot notation for nested fields (e.g., "personalInfo.msisdn", "billing.billcycleDay").
     * 
     * Supported fields depend on Subscriber entity structure. State transitions
     * should use dedicated state management endpoints if available.
     *
     * @param subscriberId Subscriber UUID
     * @param patchOperation List of patch operations (fieldName, fieldValue)
     * @return Subscriber patched (status code 200)
     *         or Bad request (status code 400)
     *         or Resource not found (status code 404)
     *         or Field to patch not found (status code 422)
     */
    @PatchMapping(value = "/{subscriberId}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<com.telecom.ocs.provisioning.api.model.Subscriber> subscribersSubscriberIdPatch(
            @PathVariable("subscriberId") String subscriberId,
            @Valid @Size(min = 1) @RequestBody List<PatchOperation> patchOperation) {
        
        log.info("Received PATCH request for subscriber ID: {} with {} operations", 
                subscriberId, patchOperation.size());
        
        try {
            // Apply patch operations in service layer (transactional)
            Subscriber updated = subscriberService.patchSubscriber(subscriberId, patchOperation);
            
            // Convert to DTO
            com.telecom.ocs.provisioning.api.model.Subscriber responseDto = subscriberMapper.toDto(updated);
            
            log.info("Successfully patched subscriber with ID: {}", subscriberId);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            log.error("Invalid patch operation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    /**
     * DELETE /subscribers/{subscriberId} : Delete subscriber
     * 
     * Deletes a subscriber and all associated data (cascade delete).
     * This is a hard delete operation.
     *
     * @param subscriberId Subscriber UUID
     * @return Subscriber deleted (status code 204)
     */
    @DeleteMapping(value = "/{subscriberId}")
    public ResponseEntity<Void> subscribersSubscriberIdDelete(
            @PathVariable("subscriberId") String subscriberId) {
        
        log.info("Received request to delete subscriber with ID: {}", subscriberId);
        
        // Delete subscriber (service throws ResourceNotFoundException if not found)
        subscriberService.deleteSubscriber(subscriberId);
        
        log.info("Successfully deleted subscriber with ID: {}", subscriberId);
        return ResponseEntity.noContent().build();
    }
}
