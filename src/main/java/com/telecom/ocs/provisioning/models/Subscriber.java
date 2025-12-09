package com.telecom.ocs.provisioning.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a telecom subscriber in the online charging system.
 * 
 * Lifecycle states: PRE_PROVISIONED → ACTIVE → SUSPENDED → DEACTIVATED → TERMINATED
 * 
 * Implements FR-006 (msisdn pattern validation), FR-007 (msisdn uniqueness),
 * FR-004 (state transition tracking)
 */
@Entity
@Table(name = "subscribers",
        indexes = {
                @Index(name = "idx_subscriber_msisdn", columnList = "msisdn"),
                @Index(name = "idx_subscriber_imsi", columnList = "imsi"),
                @Index(name = "idx_subscriber_name", columnList = "first_name, last_name"),
                @Index(name = "idx_subscriber_state", columnList = "state")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscriber {

    /**
     * Lifecycle states for subscriber entity
     */
    public enum SubscriberState {
        PRE_PROVISIONED,
        ACTIVE,
        SUSPENDED,
        DEACTIVATED,
        TERMINATED
    }

    @Id
    @Column(name = "subscriber_id", length = 36, nullable = false)
    private String subscriberId;

    /**
     * International phone number (11-15 digits)
     * FR-006: Pattern validation ^[0-9]{11,15}$
     * FR-007: Unique constraint (excluding deactivated state)
     */
    @NotNull(message = "msisdn is required")
    @Pattern(regexp = "^[0-9]{11,15}$", message = "msisdn must be 11-15 digits")
    @Column(name = "msisdn", length = 15, nullable = false)
    private String msisdn;

    /**
     * International Mobile Subscriber Identity (15 digits)
     */
    @Column(name = "imsi", length = 15)
    private String imsi;

    @Column(name = "language_id", length = 50)
    private String languageId;

    @Column(name = "carrier_id", length = 50)
    private String carrierId;

    @Column(name = "subscriber_type", length = 50)
    private String subscriberType;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Email(message = "email must be valid")
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    // Billing information
    @Column(name = "billing_cycle", length = 50)
    private String billingCycle;

    @Column(name = "billcycle_day")
    private Integer billcycleDay;

    @Column(name = "billing_street", length = 255)
    private String billingStreet;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state", length = 100)
    private String billingState;

    @Column(name = "billing_zip_code", length = 20)
    private String billingZipCode;

    @Column(name = "billing_country", length = 100)
    private String billingCountry;

    /**
     * Service entitlements (voice, SMS, data, roaming, VAS)
     * Stored as JSON string
     */
    @Column(name = "services", columnDefinition = "TEXT")
    private String services;

    /**
     * Current lifecycle state
     * FR-004: Track state transitions
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", length = 20, nullable = false)
    @Builder.Default
    private SubscriberState state = SubscriberState.PRE_PROVISIONED;

    /**
     * Previous lifecycle state (before last transition)
     * FR-004: Track previous state for audit
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state", length = 20)
    private SubscriberState previousState;

    /**
     * Timestamp of last state transition
     * FR-004: Track when state changed
     */
    @Column(name = "last_transition_date")
    private LocalDateTime lastTransitionDate;

    /**
     * Timestamp when subscriber was activated
     */
    @Column(name = "activation_date")
    private LocalDateTime activationDate;

    /**
     * Timestamp when subscriber expires
     */
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    /**
     * Timestamp when subscriber was created
     */
    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

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
     * Helper method to transition to a new state while tracking history
     * 
     * @param newState the target state
     */
    public void transitionTo(SubscriberState newState) {
        if (this.state != newState) {
            this.previousState = this.state;
            this.state = newState;
            this.lastTransitionDate = LocalDateTime.now();
        }
    }

    /**
     * Pre-persist lifecycle hook to generate UUID if not set
     */
    @PrePersist
    protected void onCreate() {
        if (this.subscriberId == null || this.subscriberId.isEmpty()) {
            this.subscriberId = java.util.UUID.randomUUID().toString();
        }
    }
}
