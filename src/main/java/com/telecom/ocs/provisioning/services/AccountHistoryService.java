package com.telecom.ocs.provisioning.services;

import com.telecom.ocs.provisioning.exceptions.ResourceNotFoundException;
import com.telecom.ocs.provisioning.models.AccountHistory;
import com.telecom.ocs.provisioning.repositories.AccountHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service layer for AccountHistory entity business logic.
 * 
 * Implements:
 * - CRUD operations for account history entries
 * - Audit logging for compliance and troubleshooting
 * - Chronological ordering of history entries
 * - Entity type validation
 * - Immutable field protection
 * 
 * T096: AccountHistoryService with audit logging logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountHistoryService {

    private final AccountHistoryRepository accountHistoryRepository;

    private static final Set<String> VALID_ENTITY_TYPES = new HashSet<>(
            Arrays.asList("SUBSCRIBER", "GROUP", "ACCOUNT"));

    // =========================================================================
    // Create Operations
    // =========================================================================

    /**
     * Create a new account history entry.
     * T096: Audit logging logic
     * 
     * @param accountHistory the account history entry to create
     * @return the created account history entry
     * @throws IllegalArgumentException if validation fails
     */
    public AccountHistory createAccountHistory(AccountHistory accountHistory) {
        log.info("Creating account history entry for entity: {} of type: {}", 
                accountHistory != null ? accountHistory.getEntityId() : "null",
                accountHistory != null ? accountHistory.getEntityType() : "null");

        // Validate input
        if (accountHistory == null) {
            log.error("Cannot create null account history entry");
            throw new IllegalArgumentException("AccountHistory cannot be null");
        }

        // Validate required fields
        validateRequiredFields(accountHistory);

        // Validate entity type
        validateEntityType(accountHistory.getEntityType());

        // Check for duplicate interaction ID
        if (accountHistoryRepository.existsById(accountHistory.getInteractionId())) {
            log.error("Account history entry with interactionId {} already exists", 
                    accountHistory.getInteractionId());
            throw new IllegalArgumentException(
                    "Account history entry with this interactionId already exists");
        }

        AccountHistory saved = accountHistoryRepository.save(accountHistory);
        log.info("Created account history entry with interactionId: {} for entity: {}", 
                saved.getInteractionId(), saved.getEntityId());

        return saved;
    }

    // =========================================================================
    // Read Operations
    // =========================================================================

    /**
     * Get account history entry by interaction ID.
     * T096: Audit logging logic
     * 
     * @param interactionId the interaction ID
     * @return optional containing the account history entry if found
     */
    @Transactional(readOnly = true)
    public Optional<AccountHistory> getAccountHistoryByInteractionId(String interactionId) {
        log.debug("Retrieving account history entry: {}", interactionId);
        
        Optional<AccountHistory> result = accountHistoryRepository.findById(interactionId);
        
        if (result.isEmpty()) {
            log.debug("Account history entry not found: {}", interactionId);
        }
        
        return result;
    }

    /**
     * List all account history entries for a given entity ID.
     * Returns entries in reverse chronological order (newest first).
     * T095: Uses custom query for chronological ordering
     * T096: Audit logging logic
     * 
     * @param entityId the entity ID to filter by
     * @return list of account history entries in reverse chronological order
     */
    @Transactional(readOnly = true)
    public List<AccountHistory> listAccountHistoryByEntityId(String entityId) {
        log.debug("Retrieving account history for entity: {}", entityId);
        
        List<AccountHistory> entries = accountHistoryRepository.findByEntityIdOrderByStartDateTimeDesc(entityId);
        
        log.debug("Found {} account history entries for entity: {}", entries.size(), entityId);
        return entries;
    }

    // =========================================================================
    // Update Operations
    // =========================================================================

    /**
     * Update an existing account history entry.
     * Protects immutable fields: interactionId, entityId, entityType, creationDate.
     * T096: Audit logging logic with immutable field protection
     * 
     * @param interactionId the interaction ID of the entry to update
     * @param updates the updates to apply
     * @return the updated account history entry
     * @throws IllegalArgumentException if entry not found
     */
    public AccountHistory updateAccountHistory(String interactionId, AccountHistory updates) {
        log.info("Updating account history entry: {}", interactionId);

        AccountHistory existing = accountHistoryRepository.findById(interactionId)
                .orElseThrow(() -> {
                    log.error("Account history entry not found for update: {}", interactionId);
                    return new IllegalArgumentException(
                            "Account history entry not found with interactionId: " + interactionId);
                });

        // Update only mutable fields
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getDirection() != null) {
            existing.setDirection(updates.getDirection());
        }
        if (updates.getReason() != null) {
            existing.setReason(updates.getReason());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        if (updates.getStatusChangeDate() != null) {
            existing.setStatusChangeDate(updates.getStatusChangeDate());
        }
        if (updates.getChannel() != null) {
            existing.setChannel(updates.getChannel());
        }
        if (updates.getStartDateTime() != null) {
            existing.setStartDateTime(updates.getStartDateTime());
        }
        if (updates.getEndDateTime() != null) {
            existing.setEndDateTime(updates.getEndDateTime());
        }

        // Note: interactionId, entityId, entityType, creationDate are immutable

        AccountHistory saved = accountHistoryRepository.save(existing);
        log.info("Updated account history entry: {}", interactionId);

        return saved;
    }

    // =========================================================================
    // Delete Operations
    // =========================================================================

    /**
     * Delete an account history entry by interaction ID.
     * T096: Audit logging logic
     * 
     * @param interactionId the interaction ID to delete
     * @throws IllegalArgumentException if entry not found
     */
    public void deleteAccountHistory(String interactionId) {
        log.info("Deleting account history entry: {}", interactionId);

        if (!accountHistoryRepository.existsById(interactionId)) {
            log.error("Account history entry not found for deletion: {}", interactionId);
            throw new IllegalArgumentException(
                    "Account history entry not found with interactionId: " + interactionId);
        }

        accountHistoryRepository.deleteById(interactionId);
        log.info("Deleted account history entry: {}", interactionId);
    }

    // =========================================================================
    // Validation Methods
    // =========================================================================

    /**
     * Validate required fields.
     * 
     * @param accountHistory the account history entry to validate
     * @throws IllegalArgumentException if required fields are missing
     */
    private void validateRequiredFields(AccountHistory accountHistory) {
        if (accountHistory.getInteractionId() == null || accountHistory.getInteractionId().isBlank()) {
            log.error("Missing required field: interactionId");
            throw new IllegalArgumentException("interactionId is required");
        }
        if (accountHistory.getEntityId() == null || accountHistory.getEntityId().isBlank()) {
            log.error("Missing required field: entityId");
            throw new IllegalArgumentException("entityId is required");
        }
        if (accountHistory.getEntityType() == null) {
            log.error("Missing required field: entityType");
            throw new IllegalArgumentException("entityType is required");
        }
        // creationDate is auto-generated by @CreationTimestamp, but validate if manually set
    }

    /**
     * Validate entity type enum value.
     * 
     * @param entityType the entity type to validate
     * @throws IllegalArgumentException if entity type is invalid
     */
    private void validateEntityType(AccountHistory.EntityType entityType) {
        if (entityType == null) {
            log.error("EntityType cannot be null");
            throw new IllegalArgumentException("entityType is required");
        }

        String typeValue = entityType.name();
        if (!VALID_ENTITY_TYPES.contains(typeValue)) {
            log.error("Invalid entityType: {}. Valid values: {}", typeValue, VALID_ENTITY_TYPES);
            throw new IllegalArgumentException(
                    "Invalid entityType: " + typeValue + ". Must be one of: " + VALID_ENTITY_TYPES);
        }
    }
}
