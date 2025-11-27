package com.telecom.ocs.provisioning.services;

import com.telecom.ocs.provisioning.exceptions.DuplicateResourceException;
import com.telecom.ocs.provisioning.exceptions.ResourceNotFoundException;
import com.telecom.ocs.provisioning.models.Subscription;
import com.telecom.ocs.provisioning.models.Subscription.SubscriptionState;
import com.telecom.ocs.provisioning.repositories.SubscriberRepository;
import com.telecom.ocs.provisioning.repositories.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for Subscription entity business logic.
 * 
 * Implements:
 * - CRUD operations for subscriptions
 * - State transition management (activate, suspend, cancel)
 * - Recurring cycle management with auto-expiration (FR-015)
 * - Renewal date calculation (FR-017)
 * - Cascade delete handling
 * - Optimistic locking handling (FR-072)
 * 
 * Supports tasks T065, T068, T069, T070
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriberRepository subscriberRepository;

    // =========================================================================
    // Create Operations
    // =========================================================================

    /**
     * Create a new subscription for a subscriber
     * 
     * @param subscriberId the subscriber ID to create subscription for
     * @param subscription the subscription to create
     * @return the created subscription with generated ID
     * @throws ResourceNotFoundException if subscriber not found
     * @throws DuplicateResourceException if subscriptionId already exists
     */
    public Subscription createSubscription(String subscriberId, Subscription subscription) {
        log.info("Creating subscription for subscriber: {} with offerId: {}", 
                subscriberId, subscription.getOfferId());

        // Validate subscriber exists
        subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> {
                    log.warn("Subscriber not found: {}", subscriberId);
                    return new ResourceNotFoundException("Subscriber not found with id: " + subscriberId);
                });

        // Check for duplicate subscriptionId if provided
        if (subscription.getSubscriptionId() != null && 
            subscriptionRepository.existsById(subscription.getSubscriptionId())) {
            log.warn("Duplicate subscriptionId: {}", subscription.getSubscriptionId());
            throw new DuplicateResourceException(
                    "Subscription with subscriptionId " + subscription.getSubscriptionId() + " already exists");
        }

        // Set subscriber reference
        subscription.setSubscriberId(subscriberId);

        // Set default state if not provided
        if (subscription.getState() == null) {
            subscription.setState(SubscriptionState.PENDING);
        }

        // Set default expiration date if not provided (2037-12-31 23:59:59)
        if (subscription.getExpirationDate() == null) {
            subscription.setExpirationDate(LocalDateTime.of(2037, 12, 31, 23, 59, 59));
        }

        // Initialize recurring cycles completed if null
        if (subscription.getRecurringCyclesCompleted() == null) {
            subscription.setRecurringCyclesCompleted(0);
        }

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Created subscription with ID: {} in state: {} for subscriber: {}", 
                saved.getSubscriptionId(), saved.getState(), subscriberId);

        return saved;
    }

    // =========================================================================
    // Read Operations
    // =========================================================================

    /**
     * Retrieve subscription by ID
     * 
     * @param subscriptionId the subscription ID
     * @return the subscription
     * @throws ResourceNotFoundException if subscription not found
     */
    @Transactional(readOnly = true)
    public Subscription getSubscriptionById(String subscriptionId) {
        log.debug("Retrieving subscription by ID: {}", subscriptionId);

        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> {
                    log.warn("Subscription not found with ID: {}", subscriptionId);
                    return new ResourceNotFoundException(
                            "Subscription not found with id: " + subscriptionId);
                });
    }

    /**
     * Get all subscriptions for a subscriber
     * 
     * @param subscriberId the subscriber ID
     * @return list of subscriptions
     * @throws ResourceNotFoundException if subscriber not found
     */
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsBySubscriberId(String subscriberId) {
        log.debug("Retrieving subscriptions for subscriber: {}", subscriberId);

        // Validate subscriber exists
        if (!subscriberRepository.existsById(subscriberId)) {
            log.warn("Subscriber not found: {}", subscriberId);
            throw new ResourceNotFoundException("Subscriber not found with id: " + subscriberId);
        }

        return subscriptionRepository.findBySubscriberId(subscriberId);
    }

    /**
     * Get all subscriptions
     * 
     * @return list of all subscriptions
     */
    @Transactional(readOnly = true)
    public List<Subscription> getAllSubscriptions() {
        log.debug("Retrieving all subscriptions");
        return subscriptionRepository.findAll();
    }

    /**
     * Get subscriptions by state
     * 
     * @param state the subscription state
     * @return list of subscriptions in the given state
     */
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByState(SubscriptionState state) {
        log.debug("Retrieving subscriptions in state: {}", state);
        return subscriptionRepository.findByState(state);
    }

    // =========================================================================
    // State Transition Operations
    // =========================================================================

    /**
     * Activate a subscription (PENDING/SUSPENDED → ACTIVE)
     * FR-017: Sets activationDate and calculates renewalDate for recurring subscriptions
     * 
     * @param subscriptionId the subscription ID
     * @return the activated subscription
     * @throws ResourceNotFoundException if subscription not found
     * @throws IllegalStateException if state transition is invalid
     */
    public Subscription activateSubscription(String subscriptionId) {
        log.info("Activating subscription: {}", subscriptionId);

        Subscription subscription = getSubscriptionById(subscriptionId);
        SubscriptionState oldState = subscription.getState();

        // Validate state transition
        if (!subscription.canActivate()) {
            log.warn("Invalid state transition for subscription {}: {} → ACTIVE", 
                    subscriptionId, oldState);
            throw new IllegalStateException(
                    "Invalid state transition from " + oldState + " to ACTIVE");
        }

        // Transition to ACTIVE state
        subscription.transitionTo(SubscriptionState.ACTIVE);
        
        // FR-017: Calculate renewal date for recurring subscriptions
        if (Boolean.TRUE.equals(subscription.getRecurring())) {
            subscription.calculateRenewalDate();
            log.debug("Set renewal date to: {} for subscription: {}", 
                    subscription.getRenewalDate(), subscriptionId);
        }

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription {} activated: {} → ACTIVE", subscriptionId, oldState);

        return saved;
    }

    /**
     * Suspend a subscription (ACTIVE → SUSPENDED)
     * 
     * @param subscriptionId the subscription ID
     * @return the suspended subscription
     * @throws ResourceNotFoundException if subscription not found
     * @throws IllegalStateException if state transition is invalid
     */
    public Subscription suspendSubscription(String subscriptionId) {
        log.info("Suspending subscription: {}", subscriptionId);

        Subscription subscription = getSubscriptionById(subscriptionId);
        SubscriptionState oldState = subscription.getState();

        // Validate state transition
        if (!subscription.canSuspend()) {
            log.warn("Invalid state transition for subscription {}: {} → SUSPENDED", 
                    subscriptionId, oldState);
            throw new IllegalStateException(
                    "Invalid state transition from " + oldState + " to SUSPENDED");
        }

        subscription.transitionTo(SubscriptionState.SUSPENDED);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription {} suspended: {} → SUSPENDED", subscriptionId, oldState);

        return saved;
    }

    /**
     * Cancel a subscription (ACTIVE/SUSPENDED → CANCELLED)
     * 
     * @param subscriptionId the subscription ID
     * @return the cancelled subscription
     * @throws ResourceNotFoundException if subscription not found
     * @throws IllegalStateException if state transition is invalid
     */
    public Subscription cancelSubscription(String subscriptionId) {
        log.info("Cancelling subscription: {}", subscriptionId);

        Subscription subscription = getSubscriptionById(subscriptionId);
        SubscriptionState oldState = subscription.getState();

        // Validate state transition
        if (!subscription.canCancel()) {
            log.warn("Invalid state transition for subscription {}: {} → CANCELLED", 
                    subscriptionId, oldState);
            throw new IllegalStateException(
                    "Invalid state transition from " + oldState + " to CANCELLED");
        }

        subscription.transitionTo(SubscriptionState.CANCELLED);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription {} cancelled: {} → CANCELLED", subscriptionId, oldState);

        return saved;
    }

    // =========================================================================
    // Recurring Cycle Operations (FR-015, FR-017)
    // =========================================================================

    /**
     * Increment the recurring cycle count and check for auto-expiration
     * FR-015: Auto-transition to EXPIRED when recurringCyclesCompleted == maxRecurringCycles
     * FR-017: Recalculates renewalDate after cycle increment
     * 
     * @param subscriptionId the subscription ID
     * @return the updated subscription
     * @throws ResourceNotFoundException if subscription not found
     * @throws IllegalStateException if subscription is not active
     */
    public Subscription incrementRecurringCycle(String subscriptionId) {
        log.info("Incrementing recurring cycle for subscription: {}", subscriptionId);

        Subscription subscription = getSubscriptionById(subscriptionId);

        // Validate subscription is active
        if (!subscription.isActive()) {
            log.warn("Cannot increment cycle for non-active subscription: {} (state: {})", 
                    subscriptionId, subscription.getState());
            throw new IllegalStateException(
                    "Cannot increment recurring cycle for subscription in state: " + subscription.getState());
        }

        // Increment cycle and check for expiration
        boolean expired = subscription.incrementRecurringCycle();
        
        if (expired) {
            log.info("Subscription {} auto-expired after {} cycles (max: {})", 
                    subscriptionId, 
                    subscription.getRecurringCyclesCompleted(),
                    subscription.getMaxRecurringCycles());
        } else {
            // Recalculate renewal date for next cycle
            subscription.calculateRenewalDate();
            log.debug("Subscription {} cycle incremented to {}, next renewal: {}", 
                    subscriptionId, 
                    subscription.getRecurringCyclesCompleted(),
                    subscription.getRenewalDate());
        }

        return subscriptionRepository.save(subscription);
    }

    /**
     * Process auto-expiration for all subscriptions that have reached max cycles
     * FR-015: Batch operation to expire subscriptions
     * 
     * @return list of expired subscriptions
     */
    public List<Subscription> processAutoExpiration() {
        log.info("Processing auto-expiration for subscriptions at max cycles");

        List<Subscription> readyForExpiration = subscriptionRepository.findReadyForExpiration();
        
        for (Subscription subscription : readyForExpiration) {
            subscription.transitionTo(SubscriptionState.EXPIRED);
            subscriptionRepository.save(subscription);
            log.info("Auto-expired subscription: {} (cycles: {}/{})", 
                    subscription.getSubscriptionId(),
                    subscription.getRecurringCyclesCompleted(),
                    subscription.getMaxRecurringCycles());
        }

        log.info("Auto-expiration complete. Expired {} subscriptions", readyForExpiration.size());
        return readyForExpiration;
    }

    /**
     * Process renewals for subscriptions due for renewal
     * FR-017: Recalculates renewal dates
     * 
     * @param asOfDate the date to check renewals against
     * @return list of renewed subscriptions
     */
    public List<Subscription> processRenewals(LocalDateTime asOfDate) {
        log.info("Processing renewals due before: {}", asOfDate);

        List<Subscription> dueForRenewal = subscriptionRepository.findDueForRenewal(asOfDate);
        
        for (Subscription subscription : dueForRenewal) {
            // Increment cycle (may auto-expire)
            boolean expired = subscription.incrementRecurringCycle();
            
            if (!expired) {
                // Recalculate next renewal date from current renewal date
                subscription.calculateRenewalDate();
            }
            
            subscriptionRepository.save(subscription);
            log.info("Processed renewal for subscription: {} (expired: {})", 
                    subscription.getSubscriptionId(), expired);
        }

        log.info("Renewal processing complete. Processed {} subscriptions", dueForRenewal.size());
        return dueForRenewal;
    }

    // =========================================================================
    // Update Operations
    // =========================================================================

    /**
     * Update subscription fields
     * 
     * @param subscriptionId the subscription ID
     * @param updates the subscription with updated fields
     * @return the updated subscription
     * @throws ResourceNotFoundException if subscription not found
     */
    public Subscription updateSubscription(String subscriptionId, Subscription updates) {
        log.info("Updating subscription: {}", subscriptionId);

        Subscription existing = getSubscriptionById(subscriptionId);

        // Update fields if provided (non-null values)
        if (updates.getOfferName() != null) {
            existing.setOfferName(updates.getOfferName());
        }
        if (updates.getSubscriptionType() != null) {
            existing.setSubscriptionType(updates.getSubscriptionType());
        }
        if (updates.getExpirationDate() != null) {
            existing.setExpirationDate(updates.getExpirationDate());
        }
        if (updates.getRecurring() != null) {
            existing.setRecurring(updates.getRecurring());
        }
        if (updates.getMaxRecurringCycles() != null) {
            existing.setMaxRecurringCycles(updates.getMaxRecurringCycles());
        }
        if (updates.getCycleLengthUnits() != null) {
            existing.setCycleLengthUnits(updates.getCycleLengthUnits());
        }
        if (updates.getCycleLengthType() != null) {
            existing.setCycleLengthType(updates.getCycleLengthType());
        }
        if (updates.getPaidFlag() != null) {
            existing.setPaidFlag(updates.getPaidFlag());
        }
        if (updates.getIsGroup() != null) {
            existing.setIsGroup(updates.getIsGroup());
        }
        if (updates.getCustomParameters() != null) {
            existing.setCustomParameters(updates.getCustomParameters());
        }

        Subscription saved = subscriptionRepository.save(existing);
        log.info("Subscription {} updated successfully", subscriptionId);

        return saved;
    }

    // =========================================================================
    // Delete Operations
    // =========================================================================

    /**
     * Delete subscription by ID
     * JPA cascade configuration handles deletion of related balances
     * 
     * @param subscriptionId the subscription ID
     * @throws ResourceNotFoundException if subscription not found
     */
    public void deleteSubscription(String subscriptionId) {
        log.info("Deleting subscription: {}", subscriptionId);

        Subscription subscription = getSubscriptionById(subscriptionId);

        // JPA cascade configuration will handle related entities (balances)
        subscriptionRepository.delete(subscription);

        log.info("Subscription {} deleted successfully", subscriptionId);
    }

    /**
     * Delete all subscriptions for a subscriber
     * Used for cascade delete when subscriber is deleted
     * 
     * @param subscriberId the subscriber ID
     */
    public void deleteSubscriptionsBySubscriberId(String subscriberId) {
        log.info("Deleting all subscriptions for subscriber: {}", subscriberId);

        subscriptionRepository.deleteBySubscriberId(subscriberId);

        log.info("All subscriptions deleted for subscriber: {}", subscriberId);
    }
}
