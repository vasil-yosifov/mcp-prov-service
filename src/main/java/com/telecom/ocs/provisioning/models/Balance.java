package com.telecom.ocs.provisioning.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a usage balance or allowance for a subscription.
 * 
 * Supports data allowances, voice minutes, monetary credits, and event counters.
 * 
 * Implements:
 * - FR-025: Expiration date checking against current date
 * - FR-030: Rollover amount capping at maxRolloverAmount
 * - FR-024: Track total (balanceAmount) vs available (balanceAvailable) separately
 */
@Entity
@Table(name = "balances",
        indexes = {
                @Index(name = "idx_balance_subscription", columnList = "subscription_id"),
                @Index(name = "idx_balance_expiration", columnList = "expiration_date")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Balance {

    /**
     * Balance type enumeration
     */
    public enum BalanceType {
        /** Prepaid allowance (decreases with usage) */
        ALLOWANCE,
        /** Counter for tracking events */
        COUNTER
    }

    /**
     * Unit type enumeration for balance measurements
     */
    public enum UnitType {
        /** Data volume in bytes */
        BYTES,
        /** Time duration in seconds */
        SECONDS,
        /** Event count */
        EVENTS,
        /** Monetary value in microcents (1/1,000,000 of currency unit) */
        MICROCENTS,
        /** Generic micro-units (1/1,000,000 of base unit) */
        MICROUNITS
    }

    @Id
    @Column(name = "balance_id", length = 255, nullable = false)
    private String balanceId;

    /**
     * Reference to parent subscription
     */
    @NotNull(message = "subscriptionId is required")
    @Column(name = "subscription_id", length = 255, nullable = false)
    private String subscriptionId;

    /**
     * Many-to-One relationship with Subscription entity
     * Balances are cascade deleted when subscription is deleted
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", insertable = false, updatable = false)
    private Subscription subscription;

    /**
     * Type of balance (ALLOWANCE or COUNTER)
     */
    @NotNull(message = "balanceType is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "balance_type", length = 50, nullable = false)
    private BalanceType balanceType;

    /**
     * Unit type for balance measurement
     */
    @NotNull(message = "unitType is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", length = 50, nullable = false)
    private UnitType unitType;

    /**
     * Initial/total balance amount
     */
    @NotNull(message = "balanceAmount is required")
    @Column(name = "balance_amount", nullable = false)
    private Long balanceAmount;

    /**
     * Remaining available balance
     * Decreases with usage, never exceeds balanceAmount
     */
    @NotNull(message = "balanceAvailable is required")
    @Column(name = "balance_available", nullable = false)
    private Long balanceAvailable;

    /**
     * Balance effective start date
     */
    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    /**
     * Balance expiration date (null = never expires)
     * FR-025: System checks this against current date to mark expired balances
     */
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    /**
     * Whether rollover is allowed at cycle boundaries
     */
    @NotNull
    @Column(name = "is_rollover_allowed", nullable = false)
    @Builder.Default
    private Boolean isRolloverAllowed = false;

    /**
     * Amount to rollover to next cycle
     * FR-030: Capped at maxRolloverAmount when cycle completes
     */
    @Column(name = "rollover_amount")
    private Long rolloverAmount;

    /**
     * Maximum amount that can be rolled over
     * FR-030: Rollover capping constraint
     */
    @Column(name = "max_rollover_amount")
    private Long maxRolloverAmount;

    /**
     * Whether balance recurs automatically at cycle boundaries
     */
    @NotNull
    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private Boolean isRecurring = false;

    /**
     * Cycle length type for recurring balances (DAYS/WEEKS/MONTHS/YEARS)
     * Only used if isRecurring = true
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_length_type", length = 50)
    private Subscription.CycleLengthType cycleLengthType;

    /**
     * Cycle length units for recurring balances
     * Only used if isRecurring = true
     */
    @Column(name = "cycle_length_units")
    private Integer cycleLengthUnits;

    /**
     * Maximum number of recurring cycles
     * Only used if isRecurring = true
     */
    @Column(name = "max_recurring_cycles")
    private Integer maxRecurringCycles;

    /**
     * Number of recurring cycles completed
     * Only used if isRecurring = true
     */
    @Column(name = "recurring_cycles_completed")
    @Builder.Default
    private Integer recurringCyclesCompleted = 0;

    /**
     * Whether balance is shared across group members
     */
    @NotNull
    @Column(name = "is_group_balance", nullable = false)
    @Builder.Default
    private Boolean isGroupBalance = false;

    /**
     * Creation timestamp (auto-populated)
     */
    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    /**
     * Last modification timestamp (auto-updated)
     */
    @UpdateTimestamp
    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    /**
     * Optimistic locking version
     * Prevents concurrent modification conflicts
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Check if balance is expired based on expiration date.
     * FR-025: Expiration date checking
     * 
     * @return true if balance has expirationDate in the past, false otherwise
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return false; // No expiration date = never expires
        }
        return LocalDateTime.now().isAfter(expirationDate);
    }

    /**
     * Check if balance is active (not expired and effective date passed).
     * 
     * @return true if balance is currently active
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        
        // Check if expired
        if (isExpired()) {
            return false;
        }
        
        // Check if effective date has passed
        if (effectiveDate != null && now.isBefore(effectiveDate)) {
            return false;
        }
        
        return true;
    }

    /**
     * Deduct usage from available balance.
     * FR-024: Track total vs available separately
     * 
     * @param amount Amount to deduct
     * @throws IllegalStateException if insufficient balance
     */
    public void deduct(Long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Deduct amount cannot be negative");
        }
        
        if (balanceAvailable < amount) {
            throw new IllegalStateException("Insufficient balance available");
        }
        
        this.balanceAvailable -= amount;
    }

    /**
     * Refund usage back to available balance.
     * FR-024: Track total vs available separately
     * 
     * @param amount Amount to refund
     * @throws IllegalStateException if refund would exceed total balance
     */
    public void refund(Long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Refund amount cannot be negative");
        }
        
        if (balanceAvailable + amount > balanceAmount) {
            throw new IllegalStateException("Refund would exceed total balance amount");
        }
        
        this.balanceAvailable += amount;
    }

    /**
     * Calculate rollover amount at cycle boundary.
     * FR-030: Cap rollover at maxRolloverAmount
     * 
     * @return Amount that can be rolled over (capped at maxRolloverAmount)
     */
    public Long calculateRollover() {
        if (!isRolloverAllowed || balanceAvailable <= 0) {
            return 0L;
        }
        
        if (maxRolloverAmount == null) {
            return balanceAvailable; // No cap
        }
        
        // Cap at maxRolloverAmount
        return Math.min(balanceAvailable, maxRolloverAmount);
    }
}
