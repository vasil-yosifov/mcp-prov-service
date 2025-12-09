package com.telecom.ocs.provisioning.repositories;

import com.telecom.ocs.provisioning.models.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for Subscription entity.
 * 
 * Provides CRUD operations and custom query methods for:
 * - Finding subscriptions by subscriber ID
 * - Finding subscriptions by state
 * - Finding subscriptions by offer ID
 * - Finding recurring subscriptions
 * - Finding subscriptions ready for auto-expiration (FR-015)
 * 
 * Custom methods support T064 (Subscription repository) and T061 (repository tests)
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    /**
     * Find all subscriptions for a specific subscriber
     * 
     * @param subscriberId the subscriber's unique identifier
     * @return List of subscriptions (may be empty)
     */
    List<Subscription> findBySubscriberId(String subscriberId);

    /**
     * Find all subscriptions in a specific lifecycle state
     * 
     * @param state the subscription lifecycle state
     * @return List of subscriptions (may be empty)
     */
    List<Subscription> findByState(Subscription.SubscriptionState state);

    /**
     * Find subscriptions for a subscriber in a specific state
     * 
     * @param subscriberId the subscriber's unique identifier
     * @param state the subscription lifecycle state
     * @return List of subscriptions (may be empty)
     */
    List<Subscription> findBySubscriberIdAndState(String subscriberId, Subscription.SubscriptionState state);

    /**
     * Find all subscriptions with a specific offer ID
     * 
     * @param offerId the offer identifier
     * @return List of subscriptions (may be empty)
     */
    List<Subscription> findByOfferId(String offerId);

    /**
     * Find all recurring or non-recurring subscriptions
     * 
     * @param recurring true for recurring, false for non-recurring
     * @return List of subscriptions (may be empty)
     */
    List<Subscription> findByRecurring(Boolean recurring);

    /**
     * Count subscriptions for a specific subscriber
     * 
     * @param subscriberId the subscriber's unique identifier
     * @return count of subscriptions
     */
    long countBySubscriberId(String subscriberId);

    /**
     * Find active recurring subscriptions that have reached their max cycles
     * Used for FR-015: Auto-expiration when recurringCyclesCompleted >= maxRecurringCycles
     * 
     * @return List of subscriptions ready for expiration
     */
    @Query("SELECT s FROM Subscription s WHERE s.state = 'ACTIVE' " +
           "AND s.recurring = true " +
           "AND s.maxRecurringCycles IS NOT NULL " +
           "AND s.recurringCyclesCompleted >= s.maxRecurringCycles")
    List<Subscription> findReadyForExpiration();

    /**
     * Find subscriptions with renewal date before a specific date
     * Used for renewal processing
     * 
     * @param date the cutoff date for renewal
     * @return List of subscriptions due for renewal
     */
    @Query("SELECT s FROM Subscription s WHERE s.state = 'ACTIVE' " +
           "AND s.recurring = true " +
           "AND s.renewalDate IS NOT NULL " +
           "AND s.renewalDate <= :date")
    List<Subscription> findDueForRenewal(@Param("date") LocalDateTime date);

    /**
     * Find subscriptions by subscriber ID and recurring flag
     * 
     * @param subscriberId the subscriber's unique identifier
     * @param recurring true for recurring, false for non-recurring
     * @return List of subscriptions (may be empty)
     */
    List<Subscription> findBySubscriberIdAndRecurring(String subscriberId, Boolean recurring);

    /**
     * Check if subscriber has any active subscriptions
     * 
     * @param subscriberId the subscriber's unique identifier
     * @return true if subscriber has at least one active subscription
     */
    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.subscriberId = :subscriberId AND s.state = 'ACTIVE'")
    boolean hasActiveSubscriptions(@Param("subscriberId") String subscriberId);

    /**
     * Find subscriptions by subscription type
     * 
     * @param subscriptionType the type of subscription (e.g., DATA, VOICE, BUNDLE)
     * @return List of subscriptions (may be empty)
     */
    List<Subscription> findBySubscriptionType(String subscriptionType);

    /**
     * Delete all subscriptions for a specific subscriber
     * Used for cascade delete when subscriber is deleted
     * 
     * @param subscriberId the subscriber's unique identifier
     */
    void deleteBySubscriberId(String subscriberId);
}
