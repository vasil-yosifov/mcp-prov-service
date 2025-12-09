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
     * T101: Enhanced audit logging for compliance
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
        
        // T101: Enhanced audit logging with key details
        log.info("AUDIT: Account history entry created - interactionId={}, entityId={}, entityType={}, description={}, status={}", 
                saved.getInteractionId(), saved.getEntityId(), saved.getEntityType(), 
                saved.getDescription(), saved.getStatus());

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
     * T100: Pagination support with limit/offset
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

    /**
     * List account history entries for a given entity ID with pagination.
     * Returns entries in reverse chronological order (newest first).
     * T100: Pagination support with limit/offset
     * 
     * @param entityId the entity ID to filter by
     * @param limit maximum number of entries to return (1-100, default 20)
     * @param offset number of entries to skip (default 0)
     * @return paginated list of account history entries in reverse chronological order
     */
    @Transactional(readOnly = true)
    public List<AccountHistory> listAccountHistoryByEntityId(String entityId, Integer limit, Integer offset) {
        log.debug("Retrieving paginated account history for entity: {} (limit={}, offset={})", 
                entityId, limit, offset);
        
        // Apply defaults and constraints
        int pageLimit = (limit != null) ? Math.min(Math.max(limit, 1), 100) : 20;
        int pageOffset = (offset != null) ? Math.max(offset, 0) : 0;
        
        // Get all entries (sorted)
        List<AccountHistory> allEntries = accountHistoryRepository.findByEntityIdOrderByStartDateTimeDesc(entityId);
        
        // Apply manual pagination
        int fromIndex = Math.min(pageOffset, allEntries.size());
        int toIndex = Math.min(pageOffset + pageLimit, allEntries.size());
        List<AccountHistory> paginatedEntries = allEntries.subList(fromIndex, toIndex);
        
        log.debug("Found {} account history entries for entity: {} (returned {} of {} total)", 
                paginatedEntries.size(), entityId, paginatedEntries.size(), allEntries.size());
        
        return paginatedEntries;
    }

    // =========================================================================
    // Update Operations
    // =========================================================================

    /**
     * Update an existing account history entry.
     * Protects immutable fields: interactionId, entityId, entityType, creationDate.
     * T096: Audit logging logic with immutable field protection
     * T101: Enhanced audit logging for compliance
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

        // Track changes for audit log
        StringBuilder changes = new StringBuilder();
        
        // Update only mutable fields
        if (updates.getDescription() != null) {
            changes.append("description: '").append(existing.getDescription())
                   .append("' -> '").append(updates.getDescription()).append("', ");
            existing.setDescription(updates.getDescription());
        }
        if (updates.getDirection() != null) {
            changes.append("direction: '").append(existing.getDirection())
                   .append("' -> '").append(updates.getDirection()).append("', ");
            existing.setDirection(updates.getDirection());
        }
        if (updates.getReason() != null) {
            changes.append("reason: '").append(existing.getReason())
                   .append("' -> '").append(updates.getReason()).append("', ");
            existing.setReason(updates.getReason());
        }
        if (updates.getStatus() != null) {
            changes.append("status: '").append(existing.getStatus())
                   .append("' -> '").append(updates.getStatus()).append("', ");
            existing.setStatus(updates.getStatus());
        }
        if (updates.getStatusChangeDate() != null) {
            existing.setStatusChangeDate(updates.getStatusChangeDate());
        }
        if (updates.getChannel() != null) {
            changes.append("channel: '").append(existing.getChannel())
                   .append("' -> '").append(updates.getChannel()).append("', ");
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
        
        // T101: Enhanced audit logging with change details
        if (changes.length() > 0) {
            log.info("AUDIT: Account history entry updated - interactionId={}, entityId={}, changes=[{}]",
                    interactionId, saved.getEntityId(), changes.toString());
        } else {
            log.info("AUDIT: Account history entry updated - interactionId={}, entityId={}, no field changes",
                    interactionId, saved.getEntityId());
        }

        return saved;
    }

    // =========================================================================
    // Delete Operations
    // =========================================================================

    /**
     * Delete an account history entry by interaction ID.
     * T096: Audit logging logic
     * T101: Enhanced audit logging for compliance
     * 
     * @param interactionId the interaction ID to delete
     * @throws IllegalArgumentException if entry not found
     */
    public void deleteAccountHistory(String interactionId) {
        log.info("Deleting account history entry: {}", interactionId);

        // Retrieve entry before deletion for audit logging
        AccountHistory entry = accountHistoryRepository.findById(interactionId)
                .orElseThrow(() -> {
                    log.error("Account history entry not found for deletion: {}", interactionId);
                    return new IllegalArgumentException(
                            "Account history entry not found with interactionId: " + interactionId);
                });

        accountHistoryRepository.deleteById(interactionId);
        
        // T101: Enhanced audit logging with deleted entry details
        log.info("AUDIT: Account history entry deleted - interactionId={}, entityId={}, entityType={}, description={}",
                interactionId, entry.getEntityId(), entry.getEntityType(), entry.getDescription());
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
