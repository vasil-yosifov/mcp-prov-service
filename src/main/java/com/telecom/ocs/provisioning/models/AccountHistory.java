package com.telecom.ocs.provisioning.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AccountHistory entity representing entries in the account_history table.
 * 
 * Tracks all interactions and events on subscriber accounts, groups, and related entities
 * for audit trails, compliance, and customer service inquiries.
 * 
 * T094: Enhanced with EntityType enum, Lombok annotations, and attachment metadata
 */
@Entity
@Table(name = "account_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountHistory {

    /**
     * Entity type enumeration for account history entries.
     */
    public enum EntityType {
        SUBSCRIBER,
        GROUP,
        ACCOUNT
    }

    @Id
    @Column(name = "interaction_id", nullable = false, length = 128)
    private String interactionId;

    @Column(name = "entity_id", nullable = false, length = 128)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 64)
    private EntityType entityType;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "direction", length = 32)
    private String direction;

    @Column(name = "reason", length = 128)
    private String reason;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "status_change_date")
    private LocalDateTime statusChangeDate;

    @Column(name = "channel", length = 100)
    private String channel;

    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    // Attachment metadata
    @Column(name = "attachment_id", length = 255)
    private String attachmentId;

    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;

    @Column(name = "attachment_type", length = 100)
    private String attachmentType;
}
