package com.telecom.ocs.provisioning.services;

import com.telecom.ocs.provisioning.exceptions.DuplicateResourceException;
import com.telecom.ocs.provisioning.exceptions.ResourceNotFoundException;
import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.repositories.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Subscriber entity business logic.
 * 
 * Implements:
 * - CRUD operations for subscribers
 * - State transition management with previousState tracking (FR-004)
 * - Duplicate msisdn validation (FR-007)
 * - Cascade delete logic
 * - Optimistic locking handling (FR-072)
 * 
 * Supports tasks T049, T052, T053, T054
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    /**
     * Create a new subscriber
     * 
     * @param subscriber the subscriber to create
     * @return the created subscriber with generated ID
     * @throws DuplicateResourceException if msisdn already exists (non-deactivated)
     */
    public Subscriber createSubscriber(Subscriber subscriber) {
        log.info("Creating new subscriber with msisdn: {}", subscriber.getMsisdn());

        // FR-007: Check msisdn uniqueness (excluding deactivated subscribers)
        if (subscriberRepository.existsByMsisdnAndNotDeactivated(subscriber.getMsisdn())) {
            log.warn("Duplicate msisdn detected: {}", subscriber.getMsisdn());
            throw new DuplicateResourceException("Subscriber with msisdn " + subscriber.getMsisdn() + " already exists");
        }

        // Set default state if not provided
        if (subscriber.getState() == null) {
            subscriber.setState(Subscriber.SubscriberState.PRE_PROVISIONED);
        }

        // Set default language_id if not provided (EN)
        if (subscriber.getLanguageId() == null || subscriber.getLanguageId().trim().isEmpty()) {
            subscriber.setLanguageId("EN");
            log.debug("Set default language_id: EN for subscriber with msisdn: {}", subscriber.getMsisdn());
        }

        // Set default subscriber_type if not provided (PREPAID)
        if (subscriber.getSubscriberType() == null || subscriber.getSubscriberType().trim().isEmpty()) {
            subscriber.setSubscriberType("PREPAID");
            log.debug("Set default subscriber_type: PREPAID for subscriber with msisdn: {}", subscriber.getMsisdn());
        }

        // Set default billing_cycle if not provided (MONTHLY)
        if (subscriber.getBillingCycle() == null || subscriber.getBillingCycle().trim().isEmpty()) {
            subscriber.setBillingCycle("MONTHLY");
            log.debug("Set default billing_cycle: MONTHLY for subscriber with msisdn: {}", subscriber.getMsisdn());
        }

        // Set default billcycle_day if not provided (1)
        if (subscriber.getBillcycleDay() == null) {
            subscriber.setBillcycleDay(1);
            log.debug("Set default billcycle_day: 1 for subscriber with msisdn: {}", subscriber.getMsisdn());
        }

        // Set default expiration date if not provided (2037-12-31 23:59:59)
        if (subscriber.getExpirationDate() == null) {
            subscriber.setExpirationDate(LocalDateTime.of(2037, 12, 31, 23, 59, 59));
            log.debug("Set default expiration date: 2037-12-31 23:59:59 for subscriber with msisdn: {}", subscriber.getMsisdn());
        }

        Subscriber saved = subscriberRepository.save(subscriber);
        log.info("Created subscriber with ID: {} in state: {}", saved.getSubscriberId(), saved.getState());
        
        return saved;
    }

    /**
     * Retrieve subscriber by ID
     * 
     * @param subscriberId the subscriber ID
     * @return the subscriber
     * @throws ResourceNotFoundException if subscriber not found
     */
    @Transactional(readOnly = true)
    public Subscriber getSubscriberById(String subscriberId) {
        log.debug("Retrieving subscriber by ID: {}", subscriberId);
        
        return subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> {
                    log.warn("Subscriber not found with ID: {}", subscriberId);
                    return new ResourceNotFoundException("Subscriber not found with id: " + subscriberId);
                });
    }

    /**
     * Lookup subscriber by msisdn
     * 
     * @param msisdn the international phone number
     * @return Optional containing subscriber if found
     */
    @Transactional(readOnly = true)
    public Optional<Subscriber> lookupByMsisdn(String msisdn) {
        log.debug("Looking up subscriber by msisdn: {}", msisdn);
        return subscriberRepository.findByMsisdn(msisdn);
    }

    /**
     * Lookup subscriber by imsi
     * 
     * @param imsi the International Mobile Subscriber Identity
     * @return Optional containing subscriber if found
     */
    @Transactional(readOnly = true)
    public Optional<Subscriber> lookupByImsi(String imsi) {
        log.debug("Looking up subscriber by imsi: {}", imsi);
        return subscriberRepository.findByImsi(imsi);
    }

    /**
     * Lookup subscribers by first and last name
     * 
     * @param firstName the first name
     * @param lastName the last name
     * @return List of matching subscribers
     */
    @Transactional(readOnly = true)
    public List<Subscriber> lookupByName(String firstName, String lastName) {
        log.debug("Looking up subscribers by name: {} {}", firstName, lastName);
        return subscriberRepository.findByFirstNameAndLastName(firstName, lastName);
    }

    /**
     * Update subscriber state with previousState tracking
     * FR-004: Track state transitions with previousState and lastTransitionDate
     * 
     * @param subscriberId the subscriber ID
     * @param newState the new state
     * @return the updated subscriber
     * @throws ResourceNotFoundException if subscriber not found
     */
    public Subscriber updateSubscriberState(String subscriberId, Subscriber.SubscriberState newState) {
        log.info("Updating subscriber {} state to: {}", subscriberId, newState);

        Subscriber subscriber = getSubscriberById(subscriberId);
        Subscriber.SubscriberState oldState = subscriber.getState();

        // Use the transitionTo helper method which tracks previousState and lastTransitionDate
        subscriber.transitionTo(newState);

        Subscriber updated = subscriberRepository.save(subscriber);
        log.info("Subscriber {} state transitioned from {} to {}", subscriberId, oldState, newState);
        
        return updated;
    }

    /**
     * Update subscriber fields (PATCH operation)
     * 
     * @param subscriberId the subscriber ID
     * @param updates the subscriber with updated fields
     * @return the updated subscriber
     * @throws ResourceNotFoundException if subscriber not found
     * @throws OptimisticLockingFailureException if concurrent modification detected
     */
    public Subscriber updateSubscriber(String subscriberId, Subscriber updates) {
        log.info("Updating subscriber: {}", subscriberId);

        Subscriber existing = getSubscriberById(subscriberId);

        // Update fields if provided (non-null values)
        if (updates.getImsi() != null) {
            existing.setImsi(updates.getImsi());
        }
        if (updates.getLanguageId() != null) {
            existing.setLanguageId(updates.getLanguageId());
        }
        if (updates.getCarrierId() != null) {
            existing.setCarrierId(updates.getCarrierId());
        }
        if (updates.getSubscriberType() != null) {
            existing.setSubscriberType(updates.getSubscriberType());
        }
        if (updates.getFirstName() != null) {
            existing.setFirstName(updates.getFirstName());
        }
        if (updates.getLastName() != null) {
            existing.setLastName(updates.getLastName());
        }
        if (updates.getDateOfBirth() != null) {
            existing.setDateOfBirth(updates.getDateOfBirth());
        }
        if (updates.getEmail() != null) {
            existing.setEmail(updates.getEmail());
        }
        if (updates.getContactNumber() != null) {
            existing.setContactNumber(updates.getContactNumber());
        }
        if (updates.getBillingCycle() != null) {
            existing.setBillingCycle(updates.getBillingCycle());
        }
        if (updates.getBillcycleDay() != null) {
            existing.setBillcycleDay(updates.getBillcycleDay());
        }
        if (updates.getBillingStreet() != null) {
            existing.setBillingStreet(updates.getBillingStreet());
        }
        if (updates.getBillingCity() != null) {
            existing.setBillingCity(updates.getBillingCity());
        }
        if (updates.getBillingState() != null) {
            existing.setBillingState(updates.getBillingState());
        }
        if (updates.getBillingZipCode() != null) {
            existing.setBillingZipCode(updates.getBillingZipCode());
        }
        if (updates.getBillingCountry() != null) {
            existing.setBillingCountry(updates.getBillingCountry());
        }
        if (updates.getServices() != null) {
            existing.setServices(updates.getServices());
        }

        // State transitions should use updateSubscriberState method
        if (updates.getState() != null && updates.getState() != existing.getState()) {
            existing.transitionTo(updates.getState());
            log.info("Subscriber {} state changed to: {}", subscriberId, updates.getState());
        }

        Subscriber updated = subscriberRepository.save(existing);
        log.info("Subscriber {} updated successfully", subscriberId);
        
        return updated;
    }

    /**
     * Save subscriber entity (for direct entity updates)
     * Used when entity is already modified and just needs to be persisted
     * 
     * @param subscriber the subscriber entity to save
     * @return the saved subscriber
     */
    public Subscriber save(Subscriber subscriber) {
        log.debug("Saving subscriber: {}", subscriber.getSubscriberId());
        return subscriberRepository.save(subscriber);
    }

    /**
     * Apply patch operations to a subscriber
     * Handles field updates within a transaction to keep entity managed
     * 
     * @param subscriberId the subscriber ID
     * @param patchOperations list of patch operations
     * @return the updated subscriber
     */
    @Transactional
    public Subscriber patchSubscriber(String subscriberId, List<com.telecom.ocs.provisioning.api.model.PatchOperation> patchOperations) {
        log.info("Applying {} patch operations to subscriber: {}", patchOperations.size(), subscriberId);
        
        Subscriber entity = getSubscriberById(subscriberId);
        
        for (com.telecom.ocs.provisioning.api.model.PatchOperation operation : patchOperations) {
            String fieldName = operation.getFieldName();
            // Extract value from JsonNullable
            Object fieldValue = operation.getFieldValue().isPresent() ? operation.getFieldValue().get() : null;
            
            log.debug("Applying patch - field: {}, value: {}", fieldName, fieldValue);
            
            switch (fieldName) {
                case "msisdn":
                    if (fieldValue instanceof String) {
                        entity.setMsisdn((String) fieldValue);
                    }
                    break;
                case "imsi":
                    if (fieldValue instanceof String) {
                        entity.setImsi((String) fieldValue);
                    }
                    break;
                case "languageId":
                    if (fieldValue instanceof String) {
                        entity.setLanguageId((String) fieldValue);
                    }
                    break;
                case "carrierId":
                    if (fieldValue instanceof String) {
                        entity.setCarrierId((String) fieldValue);
                    }
                    break;
                case "subscriberType":
                    if (fieldValue instanceof String) {
                        entity.setSubscriberType((String) fieldValue);
                    }
                    break;
                case "firstName":
                    if (fieldValue instanceof String) {
                        entity.setFirstName((String) fieldValue);
                    }
                    break;
                case "lastName":
                    if (fieldValue instanceof String) {
                        entity.setLastName((String) fieldValue);
                    }
                    break;
                case "email":
                    if (fieldValue instanceof String) {
                        entity.setEmail((String) fieldValue);
                    }
                    break;
                case "contactNumber":
                    if (fieldValue instanceof String) {
                        entity.setContactNumber((String) fieldValue);
                    }
                    break;
                case "billingCycle":
                    if (fieldValue instanceof String) {
                        entity.setBillingCycle((String) fieldValue);
                    }
                    break;
                case "billcycleDay":
                    if (fieldValue instanceof Integer) {
                        entity.setBillcycleDay((Integer) fieldValue);
                    }
                    break;
                case "billingStreet":
                    if (fieldValue instanceof String) {
                        entity.setBillingStreet((String) fieldValue);
                    }
                    break;
                case "billingCity":
                    if (fieldValue instanceof String) {
                        entity.setBillingCity((String) fieldValue);
                    }
                    break;
                case "billingState":
                    if (fieldValue instanceof String) {
                        entity.setBillingState((String) fieldValue);
                    }
                    break;
                case "billingZipCode":
                    if (fieldValue instanceof String) {
                        entity.setBillingZipCode((String) fieldValue);
                    }
                    break;
                case "billingCountry":
                    if (fieldValue instanceof String) {
                        entity.setBillingCountry((String) fieldValue);
                    }
                    break;
                case "state":
                    log.warn("State update via PATCH - consider using dedicated state transition endpoint");
                    if (fieldValue instanceof String) {
                        try {
                            Subscriber.SubscriberState newState = Subscriber.SubscriberState.valueOf(((String) fieldValue).toUpperCase());
                            entity.transitionTo(newState);
                        } catch (IllegalArgumentException e) {
                            log.error("Invalid state value: {}", fieldValue, e);
                            throw new IllegalArgumentException("Invalid state value: " + fieldValue);
                        }
                    }
                    break;
                default:
                    log.warn("Unsupported field for PATCH: {}", fieldName);
                    throw new IllegalArgumentException("Unsupported field for PATCH: " + fieldName);
            }
        }
        
        // Entity will be automatically saved at end of transaction (dirty checking)
        log.info("Successfully patched subscriber: {}", subscriberId);
        return entity;
    }

    /**
     * Delete subscriber by ID
     * Cascade deletes related entities (subscriptions, notification addresses, timers)
     * 
     * @param subscriberId the subscriber ID
     * @throws ResourceNotFoundException if subscriber not found
     */
    public void deleteSubscriber(String subscriberId) {
        log.info("Deleting subscriber: {}", subscriberId);

        Subscriber subscriber = getSubscriberById(subscriberId);
        
        // JPA cascade configuration will handle related entities
        subscriberRepository.delete(subscriber);
        
        log.info("Subscriber {} deleted successfully", subscriberId);
    }

    /**
     * Get all subscribers
     * 
     * @return List of all subscribers
     */
    @Transactional(readOnly = true)
    public List<Subscriber> getAllSubscribers() {
        log.debug("Retrieving all subscribers");
        return subscriberRepository.findAll();
    }

    /**
     * Get subscribers by state
     * 
     * @param state the lifecycle state
     * @return List of subscribers in the given state
     */
    @Transactional(readOnly = true)
    public List<Subscriber> getSubscribersByState(Subscriber.SubscriberState state) {
        log.debug("Retrieving subscribers in state: {}", state);
        return subscriberRepository.findByState(state);
    }
}
