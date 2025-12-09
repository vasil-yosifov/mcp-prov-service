package com.telecom.ocs.provisioning.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a service subscription provisioned for a subscriber.
 * 
 * Lifecycle states: PENDING → ACTIVE → SUSPENDED → CANCELLED/EXPIRED
 * 
 * Implements:
 * - FR-015: Auto-transition to EXPIRED when recurringCyclesCompleted == maxRecurringCycles
 * - FR-017: Calculate renewalDate based on cycleLengthType and cycleLengthUnits
 * - FR-019: Unique subscriptionId constraint
 */
@Entity
@Table(name = "subscriptions",
        indexes = {
                @Index(name = "idx_subscription_subscriber", columnList = "subscriber_id"),
                @Index(name = "idx_subscription_state", columnList = "state"),
                @Index(name = "idx_subscription_offer", columnList = "offer_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    /**
     * Lifecycle states for subscription entity
     */
    public enum SubscriptionState {
        PENDING,
        ACTIVE,
        SUSPENDED,
        CANCELLED,
        EXPIRED
    }

    /**
     * Cycle length type enumeration for recurring subscriptions
     */
    public enum CycleLengthType {
        DAYS,
        WEEKS,
        MONTHS,
        YEARS
    }

    @Id
    @Column(name = "subscription_id", length = 255, nullable = false)
    private String subscriptionId;

    /**
     * Reference to parent subscriber
     * Many subscriptions can belong to one subscriber
     */
    @NotNull(message = "subscriberId is required")
    @Column(name = "subscriber_id", length = 255, nullable = false)
    private String subscriberId;

    /**
     * Many-to-One relationship with Subscriber entity
     * Used for JPA joins and cascade operations
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", insertable = false, updatable = false)
    private Subscriber subscriber;

    /**
     * Type of subscription (e.g., DATA, VOICE, BUNDLE)
     */
    @Column(name = "subscription_type", length = 100)
    private String subscriptionType;

    /**
     * Offer identifier for this subscription
     */
    @NotNull(message = "offerId is required")
    @Column(name = "offer_id", length = 255, nullable = false)
    private String offerId;

    /**
     * Human-readable offer name
     */
    @Column(name = "offer_name", length = 255)
    private String offerName;

    /**
     * Current lifecycle state
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 50, nullable = false)
    @Builder.Default
    private SubscriptionState state = SubscriptionState.PENDING;

    /**
     * Timestamp when subscription was created
     */
    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    /**
     * Timestamp when subscription was activated
     */
    @Column(name = "activation_date")
    private LocalDateTime activationDate;

    /**
     * Timestamp when subscription expires
     */
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    /**
     * Next renewal date for recurring subscriptions
     * FR-017: Calculated based on cycleLengthType and cycleLengthUnits
     */
    @Column(name = "renewal_date")
    private LocalDateTime renewalDate;

    /**
     * Whether this subscription recurs
     */
    @Column(name = "recurring", nullable = false)
    @Builder.Default
    private Boolean recurring = false;

    /**
     * Whether subscription has been paid
     */
    @Column(name = "paid_flag")
    @Builder.Default
    private Boolean paidFlag = false;

    /**
     * Whether this is a group subscription
     */
    @Column(name = "is_group")
    @Builder.Default
    private Boolean isGroup = false;

    /**
     * Maximum number of recurring cycles allowed
     * FR-015: Used for auto-expiration check
     */
    @Column(name = "max_recurring_cycles")
    private Integer maxRecurringCycles;

    /**
     * Number of recurring cycles completed
     * FR-015: Auto-expire when this equals maxRecurringCycles
     */
    @Column(name = "recurring_cycles_completed")
    @Builder.Default
    private Integer recurringCyclesCompleted = 0;

    /**
     * Number of units in each cycle (e.g., 1, 7, 30)
     */
    @Column(name = "cycle_length_units")
    private Integer cycleLengthUnits;

    /**
     * Type of cycle length (DAYS, WEEKS, MONTHS, YEARS)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_length_type", length = 50)
    private CycleLengthType cycleLengthType;

    /**
     * Custom parameters stored as JSON
     */
    @Column(name = "custom_parameters", columnDefinition = "JSON")
    private String customParameters;

    /**
     * Timestamp of last modification (updated automatically)
     */
    @UpdateTimestamp
    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    /**
     * Optimistic locking version field
     * FR-072: Optimistic locking for concurrent updates
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Helper method to transition to a new state
     * 
     * @param newState the target state
     */
    public void transitionTo(SubscriptionState newState) {
        this.state = newState;
        if (newState == SubscriptionState.ACTIVE && this.activationDate == null) {
            this.activationDate = LocalDateTime.now();
        }
    }

    /**
     * Increment recurring cycle count and check for auto-expiration
     * FR-015: Auto-transition to EXPIRED when recurringCyclesCompleted == maxRecurringCycles
     * 
     * @return true if subscription has expired after increment
     */
    public boolean incrementRecurringCycle() {
        if (this.recurringCyclesCompleted == null) {
            this.recurringCyclesCompleted = 0;
        }
        this.recurringCyclesCompleted++;
        
        // FR-015: Check if max cycles reached
        if (this.maxRecurringCycles != null && 
            this.recurringCyclesCompleted >= this.maxRecurringCycles) {
            this.state = SubscriptionState.EXPIRED;
            return true;
        }
        return false;
    }

    /**
     * Calculate and set the renewal date based on cycle configuration
     * FR-017: Calculate renewalDate based on cycleLengthType and cycleLengthUnits
     */
    public void calculateRenewalDate() {
        if (!Boolean.TRUE.equals(this.recurring) || this.cycleLengthType == null || this.cycleLengthUnits == null) {
            this.renewalDate = null;
            return;
        }

        LocalDateTime baseDate = this.activationDate != null ? this.activationDate : LocalDateTime.now();
        
        this.renewalDate = switch (this.cycleLengthType) {
            case DAYS -> baseDate.plusDays(this.cycleLengthUnits);
            case WEEKS -> baseDate.plusWeeks(this.cycleLengthUnits);
            case MONTHS -> baseDate.plusMonths(this.cycleLengthUnits);
            case YEARS -> baseDate.plusYears(this.cycleLengthUnits);
        };
    }

    /**
     * Check if the subscription has reached maximum recurring cycles
     * 
     * @return true if max cycles reached
     */
    public boolean hasReachedMaxCycles() {
        return this.maxRecurringCycles != null && 
               this.recurringCyclesCompleted != null &&
               this.recurringCyclesCompleted >= this.maxRecurringCycles;
    }

    /**
     * Check if the subscription is in an active state (can be used)
     * 
     * @return true if subscription is active
     */
    public boolean isActive() {
        return this.state == SubscriptionState.ACTIVE;
    }

    /**
     * Check if the subscription can be activated
     * 
     * @return true if subscription can be activated
     */
    public boolean canActivate() {
        return this.state == SubscriptionState.PENDING || this.state == SubscriptionState.SUSPENDED;
    }

    /**
     * Check if the subscription can be suspended
     * 
     * @return true if subscription can be suspended
     */
    public boolean canSuspend() {
        return this.state == SubscriptionState.ACTIVE;
    }

    /**
     * Check if the subscription can be cancelled
     * 
     * @return true if subscription can be cancelled
     */
    public boolean canCancel() {
        return this.state == SubscriptionState.ACTIVE || this.state == SubscriptionState.SUSPENDED;
    }

    /**
     * Pre-persist lifecycle hook to generate UUID if not set
     */
    @PrePersist
    protected void onCreate() {
        if (this.subscriptionId == null || this.subscriptionId.isEmpty()) {
            this.subscriptionId = java.util.UUID.randomUUID().toString();
        }
        if (this.recurringCyclesCompleted == null) {
            this.recurringCyclesCompleted = 0;
        }
    }
}
