package com.telecom.ocs.provisioning.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a usage record for service consumption.
 * 
 * Tracks voice calls, data sessions, SMS, and MMS usage by subscribers,
 * capturing the impact on balances for billing and quota enforcement.
 * 
 * Implements:
 * - FR-081 through FR-095: Usage recording requirements
 * - FR-096: ALLOWANCE balance deduction
 * - FR-097: ALLOWANCE balance floor at 0
 * - FR-098: COUNTER balance addition
 * 
 * T178: Usage JPA entity with enums and FKs
 */
@Entity
@Table(name = "usage_records",
        indexes = {
                @Index(name = "idx_usage_subscriber", columnList = "charged_party_id"),
                @Index(name = "idx_usage_balance", columnList = "impacted_balance_id"),
                @Index(name = "idx_usage_timestamp", columnList = "usage_timestamp"),
                @Index(name = "idx_usage_type", columnList = "usage_type"),
                @Index(name = "idx_usage_msisdn", columnList = "charged_msisdn"),
                @Index(name = "idx_usage_offer", columnList = "offer_id"),
                @Index(name = "idx_usage_record_type", columnList = "record_type"),
                @Index(name = "idx_usage_composite_subscriber_timestamp", columnList = "charged_party_id, usage_timestamp")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usage {

    /**
     * Usage type enumeration
     */
    public enum UsageType {
        /** Voice call usage */
        VOICE,
        /** Data/internet session usage */
        DATA,
        /** Short Message Service usage */
        SMS,
        /** Multimedia Message Service usage */
        MMS
    }

    /**
     * Record type enumeration
     */
    public enum RecordType {
        /** Session start record */
        START,
        /** Intermediate record during ongoing session */
        INTERIM,
        /** Session end record */
        STOP,
        /** Single event record (e.g., SMS) */
        EVENT
    }

    @Id
    @Column(name = "usage_id", length = 255, nullable = false)
    private String usageId;

    /**
     * Record creation timestamp (FR-093)
     */
    @CreationTimestamp
    @Column(name = "usage_timestamp", nullable = false)
    private LocalDateTime usageTimestamp;

    /**
     * Subscriber ID being charged (FK to subscribers)
     * FR-091: Must exist, validated in service layer
     */
    @NotNull(message = "chargedPartyId is required")
    @Column(name = "charged_party_id", length = 255, nullable = false)
    private String chargedPartyId;

    /**
     * MSISDN being charged (11-15 digits)
     */
    @Column(name = "charged_msisdn", length = 50)
    private String chargedMsisdn;

    /**
     * Originating MSISDN (11-15 digits)
     * FR-086: A-party tracking
     */
    @Column(name = "a_party", length = 50)
    private String aParty;

    /**
     * Terminating MSISDN or APN (for data sessions)
     * FR-086: B-party tracking
     */
    @Column(name = "b_party", length = 255)
    private String bParty;

    /**
     * Type of usage (VOICE, DATA, SMS, MMS)
     * FR-083: Usage type tracking
     */
    @NotNull(message = "usageType is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", length = 50, nullable = false)
    private UsageType usageType;

    /**
     * Record type (START, INTERIM, STOP, EVENT)
     * FR-084: Record type tracking
     */
    @NotNull(message = "recordType is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", length = 50, nullable = false)
    private RecordType recordType;

    /**
     * Record opening timestamp (for session-based usage)
     * FR-090: Session timing
     */
    @Column(name = "record_opening_time")
    private LocalDateTime recordOpeningTime;

    /**
     * Record closing timestamp (for session-based usage)
     * FR-090: Session timing
     */
    @Column(name = "record_closing_time")
    private LocalDateTime recordClosingTime;

    /**
     * Duration in seconds (calculated: closing - opening)
     * FR-090: Session duration
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Usage volume consumed
     * FR-087: bytes for DATA, seconds for VOICE, count for SMS/MMS
     */
    @NotNull(message = "volumeUsage is required")
    @Column(name = "volume_usage", nullable = false)
    private Long volumeUsage;

    /**
     * Balance impacted by this usage (FK to balances)
     * FR-088: Balance reference
     */
    @NotNull(message = "impactedBalanceId is required")
    @Column(name = "impacted_balance_id", length = 255, nullable = false)
    private String impactedBalanceId;

    /**
     * Balance available value before usage
     * FR-089: Balance impact auditing
     */
    @Column(name = "balance_value_before")
    private Long balanceValueBefore;

    /**
     * Balance available value after usage
     * FR-089: Balance impact auditing
     * T198: Captures balance change
     */
    @Column(name = "balance_value_after")
    private Long balanceValueAfter;

    /**
     * Associated offer ID (optional)
     * FR-094: Offer association
     */
    @Column(name = "offer_id", length = 255)
    private String offerId;

    /**
     * Audit timestamps
     */
    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    /**
     * Optimistic locking version
     * FR-072: Optimistic locking support
     */
    @Version
    @Column(name = "version")
    private Integer version;
}
