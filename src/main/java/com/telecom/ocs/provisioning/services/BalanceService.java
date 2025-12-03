package com.telecom.ocs.provisioning.services;

import com.telecom.ocs.provisioning.exceptions.ResourceNotFoundException;
import com.telecom.ocs.provisioning.models.Balance;
import com.telecom.ocs.provisioning.repositories.BalanceRepository;
import com.telecom.ocs.provisioning.repositories.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for Balance entity business logic.
 * 
 * Implements:
 * - CRUD operations for balances
 * - Rollover logic with maxRolloverAmount capping (FR-030, T084)
 * - Expiration date validation (FR-025, T085)
 * - Group balance sharing logic (T086)
 * - Balance deduction and refund operations
 * - Logging for balance operations (T087)
 * 
 * Supports tasks T081, T084, T085, T086, T087
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final SubscriptionRepository subscriptionRepository;

    // =========================================================================
    // Create Operations
    // =========================================================================

    /**
     * Create a new balance for a subscription.
     * T087: Logging for balance operations
     * 
     * @param subscriptionId the subscription ID to create balance for
     * @param balance the balance to create
     * @return the created balance with generated ID
     * @throws ResourceNotFoundException if subscription not found
     */
    public Balance createBalance(String subscriptionId, Balance balance) {
        log.info("Creating balance for subscription: {} with type: {} and unit: {}", 
                subscriptionId, balance.getBalanceType(), balance.getUnitType());

        // Validate subscription exists
        subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> {
                    log.warn("Subscription not found: {}", subscriptionId);
                    return new ResourceNotFoundException("Subscription not found with id: " + subscriptionId);
                });

        // Set subscription reference
        balance.setSubscriptionId(subscriptionId);

        // T085: Validate expiration date
        if (balance.getExpirationDate() != null && balance.getExpirationDate().isBefore(LocalDateTime.now())) {
            log.warn("Balance created with past expiration date: {} for subscription: {}", 
                    balance.getExpirationDate(), subscriptionId);
        }

        // Initialize rollover amount if rollover is allowed
        if (Boolean.TRUE.equals(balance.getIsRolloverAllowed()) && balance.getRolloverAmount() == null) {
            balance.setRolloverAmount(0L);
        }

        Balance saved = balanceRepository.save(balance);
        log.info("Created balance with ID: {} for subscription: {} - Amount: {}, Available: {}", 
                saved.getBalanceId(), subscriptionId, saved.getBalanceAmount(), saved.getBalanceAvailable());

        return saved;
    }

    // =========================================================================
    // Read Operations
    // =========================================================================

    /**
     * Get balance by ID.
     * T087: Logging for balance operations
     * 
     * @param balanceId the balance ID
     * @return the balance
     * @throws ResourceNotFoundException if balance not found
     */
    @Transactional(readOnly = true)
    public Balance getBalanceById(String balanceId) {
        log.debug("Retrieving balance: {}", balanceId);
        
        return balanceRepository.findById(balanceId)
                .orElseThrow(() -> {
                    log.warn("Balance not found: {}", balanceId);
                    return new ResourceNotFoundException("Balance not found with id: " + balanceId);
                });
    }

    /**
     * Get all balances for a subscription.
     * T087: Logging for balance operations
     * 
     * @param subscriptionId the subscription ID
     * @return list of balances (may be empty)
     */
    @Transactional(readOnly = true)
    public List<Balance> getBalancesBySubscriptionId(String subscriptionId) {
        log.debug("Retrieving balances for subscription: {}", subscriptionId);
        
        List<Balance> balances = balanceRepository.findBySubscriptionId(subscriptionId);
        log.debug("Found {} balances for subscription: {}", balances.size(), subscriptionId);
        
        return balances;
    }

    /**
     * Get active balances for a subscription (not expired, effective date passed).
     * T085: Expiration date validation
     * T087: Logging for balance operations
     * 
     * @param subscriptionId the subscription ID
     * @return list of active balances (may be empty)
     */
    @Transactional(readOnly = true)
    public List<Balance> getActiveBalances(String subscriptionId) {
        log.debug("Retrieving active balances for subscription: {}", subscriptionId);
        
        LocalDateTime now = LocalDateTime.now();
        List<Balance> activeBalances = balanceRepository.findActiveBalances(subscriptionId, now);
        
        log.debug("Found {} active balances for subscription: {}", activeBalances.size(), subscriptionId);
        return activeBalances;
    }

    /**
     * Get group balances for a subscription.
     * T086: Group balance sharing logic
     * T087: Logging for balance operations
     * 
     * @param subscriptionId the subscription ID
     * @return list of group balances (may be empty)
     */
    @Transactional(readOnly = true)
    public List<Balance> getGroupBalances(String subscriptionId) {
        log.debug("Retrieving group balances for subscription: {}", subscriptionId);
        
        List<Balance> groupBalances = balanceRepository.findGroupBalances(subscriptionId);
        log.debug("Found {} group balances for subscription: {}", groupBalances.size(), subscriptionId);
        
        return groupBalances;
    }

    // =========================================================================
    // Update Operations
    // =========================================================================

    /**
     * Update an existing balance.
     * T085: Expiration date validation
     * T087: Logging for balance operations
     * 
     * @param balanceId the balance ID to update
     * @param updatedBalance the updated balance data
     * @return the updated balance
     * @throws ResourceNotFoundException if balance not found
     */
    public Balance updateBalance(String balanceId, Balance updatedBalance) {
        log.info("Updating balance: {}", balanceId);

        Balance existingBalance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> {
                    log.warn("Balance not found for update: {}", balanceId);
                    return new ResourceNotFoundException("Balance not found with id: " + balanceId);
                });

        // Update mutable fields
        if (updatedBalance.getBalanceAmount() != null) {
            existingBalance.setBalanceAmount(updatedBalance.getBalanceAmount());
        }
        if (updatedBalance.getBalanceAvailable() != null) {
            existingBalance.setBalanceAvailable(updatedBalance.getBalanceAvailable());
        }
        if (updatedBalance.getExpirationDate() != null) {
            // T085: Validate expiration date
            if (updatedBalance.getExpirationDate().isBefore(LocalDateTime.now())) {
                log.warn("Updating balance with past expiration date: {} for balance: {}", 
                        updatedBalance.getExpirationDate(), balanceId);
            }
            existingBalance.setExpirationDate(updatedBalance.getExpirationDate());
        }
        if (updatedBalance.getEffectiveDate() != null) {
            existingBalance.setEffectiveDate(updatedBalance.getEffectiveDate());
        }
        if (updatedBalance.getIsRolloverAllowed() != null) {
            existingBalance.setIsRolloverAllowed(updatedBalance.getIsRolloverAllowed());
        }
        if (updatedBalance.getMaxRolloverAmount() != null) {
            existingBalance.setMaxRolloverAmount(updatedBalance.getMaxRolloverAmount());
        }
        if (updatedBalance.getIsGroupBalance() != null) {
            existingBalance.setIsGroupBalance(updatedBalance.getIsGroupBalance());
        }

        Balance saved = balanceRepository.save(existingBalance);
        log.info("Updated balance: {} - New available: {}", balanceId, saved.getBalanceAvailable());
        
        return saved;
    }

    // =========================================================================
    // Delete Operations
    // =========================================================================

    /**
     * Delete a balance by ID.
     * T087: Logging for balance operations
     * 
     * @param balanceId the balance ID to delete
     * @throws ResourceNotFoundException if balance not found
     */
    public void deleteBalance(String balanceId) {
        log.info("Deleting balance: {}", balanceId);

        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> {
                    log.warn("Balance not found for deletion: {}", balanceId);
                    return new ResourceNotFoundException("Balance not found with id: " + balanceId);
                });

        balanceRepository.delete(balance);
        log.info("Deleted balance: {} for subscription: {}", balanceId, balance.getSubscriptionId());
    }

    // =========================================================================
    // Balance Operations (Deduct, Refund, Rollover)
    // =========================================================================

    /**
     * Deduct usage from a balance.
     * T087: Logging for balance operations
     * 
     * @param balanceId the balance ID
     * @param amount the amount to deduct
     * @return the updated balance
     * @throws ResourceNotFoundException if balance not found
     * @throws IllegalStateException if insufficient balance or balance expired
     */
    public Balance deductBalance(String balanceId, Long amount) {
        log.info("Deducting {} from balance: {}", amount, balanceId);

        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> {
                    log.warn("Balance not found for deduction: {}", balanceId);
                    return new ResourceNotFoundException("Balance not found with id: " + balanceId);
                });

        // T085: Check if balance is expired
        if (balance.isExpired()) {
            log.warn("Cannot deduct from expired balance: {}", balanceId);
            throw new IllegalStateException("Balance is expired and cannot be used");
        }

        // Check if balance is active (effective date passed)
        if (!balance.isActive()) {
            log.warn("Cannot deduct from inactive balance: {}", balanceId);
            throw new IllegalStateException("Balance is not yet active or has expired");
        }

        // Perform deduction (throws IllegalStateException if insufficient)
        balance.deduct(amount);

        Balance saved = balanceRepository.save(balance);
        log.info("Deducted {} from balance: {} - Remaining: {}", 
                amount, balanceId, saved.getBalanceAvailable());
        
        return saved;
    }

    /**
     * Refund usage back to a balance.
     * T087: Logging for balance operations
     * 
     * @param balanceId the balance ID
     * @param amount the amount to refund
     * @return the updated balance
     * @throws ResourceNotFoundException if balance not found
     * @throws IllegalStateException if refund would exceed total balance
     */
    public Balance refundBalance(String balanceId, Long amount) {
        log.info("Refunding {} to balance: {}", amount, balanceId);

        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> {
                    log.warn("Balance not found for refund: {}", balanceId);
                    return new ResourceNotFoundException("Balance not found with id: " + balanceId);
                });

        // Perform refund (throws IllegalStateException if would exceed total)
        balance.refund(amount);

        Balance saved = balanceRepository.save(balance);
        log.info("Refunded {} to balance: {} - New available: {}", 
                amount, balanceId, saved.getBalanceAvailable());
        
        return saved;
    }

    /**
     * Process rollover for a balance at cycle boundary.
     * T084: Rollover amount capping logic (FR-030)
     * T087: Logging for balance operations
     * 
     * @param balanceId the balance ID
     * @return the updated balance with rollover processed
     * @throws ResourceNotFoundException if balance not found
     * @throws IllegalStateException if rollover not allowed
     */
    public Balance processRollover(String balanceId) {
        log.info("Processing rollover for balance: {}", balanceId);

        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> {
                    log.warn("Balance not found for rollover: {}", balanceId);
                    return new ResourceNotFoundException("Balance not found with id: " + balanceId);
                });

        // Check if rollover is allowed
        if (!Boolean.TRUE.equals(balance.getIsRolloverAllowed())) {
            log.warn("Rollover not allowed for balance: {}", balanceId);
            throw new IllegalStateException("Rollover is not allowed for this balance");
        }

        // T084: Calculate rollover amount (capped at maxRolloverAmount)
        Long rolloverAmount = balance.calculateRollover();
        balance.setRolloverAmount(rolloverAmount);

        Balance saved = balanceRepository.save(balance);
        log.info("Processed rollover for balance: {} - Rollover amount: {} (capped at: {})", 
                balanceId, rolloverAmount, balance.getMaxRolloverAmount());
        
        return saved;
    }

    /**
     * Check if a balance is expired.
     * T085: Expiration date validation
     * T087: Logging for balance operations
     * 
     * @param balanceId the balance ID
     * @return true if balance is expired, false otherwise
     * @throws ResourceNotFoundException if balance not found
     */
    @Transactional(readOnly = true)
    public boolean isBalanceExpired(String balanceId) {
        log.debug("Checking expiration for balance: {}", balanceId);

        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> {
                    log.warn("Balance not found for expiration check: {}", balanceId);
                    return new ResourceNotFoundException("Balance not found with id: " + balanceId);
                });

        boolean expired = balance.isExpired();
        log.debug("Balance {} expired status: {}", balanceId, expired);
        
        return expired;
    }
}
