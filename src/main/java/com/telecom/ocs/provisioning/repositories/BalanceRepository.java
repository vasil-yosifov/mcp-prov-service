package com.telecom.ocs.provisioning.repositories;

import com.telecom.ocs.provisioning.models.Balance;
import com.telecom.ocs.provisioning.models.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for Balance entity.
 * 
 * Provides CRUD operations and custom query methods for:
 * - Finding balances by subscription ID
 * - Finding active balances (not expired)
 * - Finding group balances
 * - Finding rollover-eligible balances
 * - Finding balances by type and unit type
 * 
 * Custom methods support T077 (Balance repository tests) and T080 (repository implementation)
 */
@Repository
public interface BalanceRepository extends JpaRepository<Balance, String> {

    /**
     * Find all balances for a specific subscription (by subscription entity)
     * 
     * @param subscription the parent subscription entity
     * @return List of balances (may be empty)
     */
    List<Balance> findBySubscription(Subscription subscription);

    /**
     * Find all balances for a specific subscription (by subscription ID)
     * 
     * @param subscriptionId the subscription's unique identifier
     * @return List of balances (may be empty)
     */
    List<Balance> findBySubscriptionId(String subscriptionId);

    /**
     * Find active balances for a subscription (not expired and effective date passed).
     * FR-025: Expiration date checking
     * 
     * @param subscriptionId the subscription's unique identifier
     * @param currentDate the current date/time for expiration checking
     * @return List of active balances (may be empty)
     */
    @Query("SELECT b FROM Balance b WHERE b.subscriptionId = :subscriptionId " +
           "AND (b.expirationDate IS NULL OR b.expirationDate > :currentDate) " +
           "AND (b.effectiveDate IS NULL OR b.effectiveDate <= :currentDate)")
    List<Balance> findActiveBalances(@Param("subscriptionId") String subscriptionId, 
                                     @Param("currentDate") LocalDateTime currentDate);

    /**
     * Find group balances for a subscription.
     * Group balances are shared across group members.
     * 
     * @param subscriptionId the subscription's unique identifier
     * @return List of group balances (may be empty)
     */
    @Query("SELECT b FROM Balance b WHERE b.subscriptionId = :subscriptionId " +
           "AND b.isGroupBalance = true")
    List<Balance> findGroupBalances(@Param("subscriptionId") String subscriptionId);

    /**
     * Find balances eligible for rollover at cycle boundaries.
     * FR-030: Rollover amount capping at maxRolloverAmount
     * 
     * @param subscriptionId the subscription's unique identifier
     * @return List of rollover-eligible balances (may be empty)
     */
    @Query("SELECT b FROM Balance b WHERE b.subscriptionId = :subscriptionId " +
           "AND b.isRolloverAllowed = true " +
           "AND b.balanceAvailable > 0")
    List<Balance> findRolloverEligibleBalances(@Param("subscriptionId") String subscriptionId);

    /**
     * Find balances by balance type for a subscription
     * 
     * @param subscriptionId the subscription's unique identifier
     * @param balanceType the balance type (ALLOWANCE or COUNTER)
     * @return List of balances (may be empty)
     */
    List<Balance> findBySubscriptionIdAndBalanceType(String subscriptionId, Balance.BalanceType balanceType);

    /**
     * Find balances by unit type for a subscription
     * 
     * @param subscriptionId the subscription's unique identifier
     * @param unitType the unit type (BYTES, SECONDS, etc.)
     * @return List of balances (may be empty)
     */
    List<Balance> findBySubscriptionIdAndUnitType(String subscriptionId, Balance.UnitType unitType);

    /**
     * Find expired balances for a subscription
     * FR-025: Expiration date checking
     * 
     * @param subscriptionId the subscription's unique identifier
     * @param currentDate the current date/time for expiration checking
     * @return List of expired balances (may be empty)
     */
    @Query("SELECT b FROM Balance b WHERE b.subscriptionId = :subscriptionId " +
           "AND b.expirationDate IS NOT NULL " +
           "AND b.expirationDate <= :currentDate")
    List<Balance> findExpiredBalances(@Param("subscriptionId") String subscriptionId, 
                                      @Param("currentDate") LocalDateTime currentDate);

    /**
     * Find recurring balances for a subscription
     * 
     * @param subscriptionId the subscription's unique identifier
     * @return List of recurring balances (may be empty)
     */
    @Query("SELECT b FROM Balance b WHERE b.subscriptionId = :subscriptionId " +
           "AND b.isRecurring = true")
    List<Balance> findRecurringBalances(@Param("subscriptionId") String subscriptionId);

    /**
     * Count balances for a specific subscription
     * 
     * @param subscriptionId the subscription's unique identifier
     * @return count of balances
     */
    long countBySubscriptionId(String subscriptionId);

    /**
     * Count active balances for a specific subscription
     * 
     * @param subscriptionId the subscription's unique identifier
     * @param currentDate the current date/time for expiration checking
     * @return count of active balances
     */
    @Query("SELECT COUNT(b) FROM Balance b WHERE b.subscriptionId = :subscriptionId " +
           "AND (b.expirationDate IS NULL OR b.expirationDate > :currentDate) " +
           "AND (b.effectiveDate IS NULL OR b.effectiveDate <= :currentDate)")
    long countActiveBalances(@Param("subscriptionId") String subscriptionId, 
                             @Param("currentDate") LocalDateTime currentDate);

    /**
     * Find balances with available amount greater than zero
     * 
     * @param subscriptionId the subscription's unique identifier
     * @return List of balances with available balance (may be empty)
     */
    @Query("SELECT b FROM Balance b WHERE b.subscriptionId = :subscriptionId " +
           "AND b.balanceAvailable > 0")
    List<Balance> findBalancesWithAvailableAmount(@Param("subscriptionId") String subscriptionId);

    /**
     * Delete all balances for a specific subscription
     * Used for cascade delete when subscription is deleted
     * 
     * @param subscriptionId the subscription's unique identifier
     */
    void deleteBySubscriptionId(String subscriptionId);
}
