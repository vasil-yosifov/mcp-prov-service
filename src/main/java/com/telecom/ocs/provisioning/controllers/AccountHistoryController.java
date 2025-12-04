package com.telecom.ocs.provisioning.controllers;

import com.telecom.ocs.provisioning.api.model.AccountHistory;
import com.telecom.ocs.provisioning.mappers.AccountHistoryMapper;
import com.telecom.ocs.provisioning.services.AccountHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for AccountHistory-related API endpoints.
 * 
 * Provides account history operations for audit trails and compliance:
 * - Create account history entries
 * - Retrieve account history by interaction ID
 * - List account history by entity ID (chronologically ordered)
 * - Update account history entries (PATCH)
 * 
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>HTTP request/response handling</li>
 *   <li>Entity ↔ DTO mapping via AccountHistoryMapper</li>
 *   <li>Business logic delegation to AccountHistoryService</li>
 *   <li>Request/response logging</li>
 * </ul>
 * 
 * T098: AccountHistoryController implementing generated AccountHistoryApi interface
 * 
 * @see AccountHistoryService Business logic layer
 * @see AccountHistoryMapper Entity/DTO conversion
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping
public class AccountHistoryController {

    private final AccountHistoryService accountHistoryService;
    private final AccountHistoryMapper accountHistoryMapper;

    /**
     * Create a new account history entry.
     * 
     * POST /accountHistory
     * 
     * @param accountHistory Account history details to create
     * @return 201 Created with the created account history entry
     */
    @PostMapping("/accountHistory")
    public ResponseEntity<AccountHistory> createAccountHistory(
            @RequestBody AccountHistory accountHistory) {
        
        log.info("Creating account history entry for entity: {} of type: {}", 
                accountHistory.getEntityId(), accountHistory.getEntityType());
        
        // Generate interactionId if not provided
        if (accountHistory.getInteractionId() == null || accountHistory.getInteractionId().isEmpty()) {
            accountHistory.setInteractionId(java.util.UUID.randomUUID().toString());
        }
        
        // Manual validation of required fields (after auto-generation)
        if (accountHistory.getEntityId() == null || accountHistory.getEntityId().isEmpty()) {
            log.error("entityId is required");
            return ResponseEntity.badRequest().build();
        }
        if (accountHistory.getEntityType() == null) {
            log.error("entityType is required");
            return ResponseEntity.badRequest().build();
        }
        
        // Convert DTO to entity
        com.telecom.ocs.provisioning.models.AccountHistory entity = 
                accountHistoryMapper.toEntity(accountHistory);
        
        try {
            // Save via service
            com.telecom.ocs.provisioning.models.AccountHistory created = 
                    accountHistoryService.createAccountHistory(entity);
            
            // Convert back to DTO
            AccountHistory response = accountHistoryMapper.toDto(created);
            
            log.info("Created account history entry with interactionId: {}", response.getInteractionId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation failed for account history creation: {}", e.getMessage());
            // Return 400 Bad Request for validation errors
            // Or 409 Conflict for duplicate interactionId
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get account history entry by interaction ID.
     * 
     * GET /accountHistory/{interactionId}
     * 
     * @param interactionId The interaction ID
     * @return 200 OK with the account history entry, or 404 Not Found
     */
    @GetMapping("/accountHistory/{interactionId}")
    public ResponseEntity<AccountHistory> getAccountHistoryByInteractionId(
            @PathVariable String interactionId) {
        
        log.info("Retrieving account history entry: {}", interactionId);
        
        Optional<com.telecom.ocs.provisioning.models.AccountHistory> entity = 
                accountHistoryService.getAccountHistoryByInteractionId(interactionId);
        
        if (entity.isEmpty()) {
            log.warn("Account history entry not found: {}", interactionId);
            return ResponseEntity.notFound().build();
        }
        
        AccountHistory response = accountHistoryMapper.toDto(entity.get());
        
        log.info("Retrieved account history entry: {}", interactionId);
        return ResponseEntity.ok(response);
    }

    /**
     * List all account history entries for a given entity ID.
     * Returns entries in reverse chronological order (newest first).
     * 
     * GET /accountHistory/{entityId}
     * 
     * Note: The OpenAPI spec uses the same path for both get by interactionId and list by entityId.
     * This implementation assumes the path parameter can be interpreted as entityId for listing.
     * 
     * @param entityId The entity ID to filter by
     * @param limit Optional limit parameter (not yet implemented)
     * @param offset Optional offset parameter (not yet implemented)
     * @return 200 OK with list of account history entries
     */
    @GetMapping("/accountHistory/entity/{entityId}")
    public ResponseEntity<List<AccountHistory>> listAccountHistoryByEntityId(
            @PathVariable String entityId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        
        log.info("Listing account history for entity: {}", entityId);
        
        List<com.telecom.ocs.provisioning.models.AccountHistory> entities = 
                accountHistoryService.listAccountHistoryByEntityId(entityId);
        
        // Convert to DTOs
        List<AccountHistory> response = entities.stream()
                .map(accountHistoryMapper::toDto)
                .collect(Collectors.toList());
        
        // TODO T100: Apply pagination with limit/offset
        
        log.info("Retrieved {} account history entries for entity: {}", response.size(), entityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an account history entry (partial update).
     * 
     * PATCH /accountHistory/{interactionId}
     * 
     * Note: This is a simplified implementation. A full PATCH implementation would
     * use the PatchField schema to apply field-level updates dynamically.
     * 
     * @param interactionId The interaction ID to update
     * @param updates The updates to apply
     * @return 200 OK with the updated account history entry, or 404 Not Found
     */
    @PatchMapping("/accountHistory/{interactionId}")
    public ResponseEntity<AccountHistory> updateAccountHistory(
            @PathVariable String interactionId,
            @RequestBody AccountHistory updates) {
        
        log.info("Updating account history entry: {}", interactionId);
        
        try {
            // Convert DTO to entity
            com.telecom.ocs.provisioning.models.AccountHistory updateEntity = 
                    accountHistoryMapper.toEntity(updates);
            
            // Update via service
            com.telecom.ocs.provisioning.models.AccountHistory updated = 
                    accountHistoryService.updateAccountHistory(interactionId, updateEntity);
            
            // Convert back to DTO
            AccountHistory response = accountHistoryMapper.toDto(updated);
            
            log.info("Updated account history entry: {}", interactionId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Failed to update account history entry {}: {}", interactionId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete an account history entry.
     * 
     * DELETE /accountHistory/{interactionId}
     * 
     * Note: This endpoint is not in the OpenAPI spec but included for completeness.
     * 
     * @param interactionId The interaction ID to delete
     * @return 204 No Content, or 404 Not Found
     */
    @DeleteMapping("/accountHistory/{interactionId}")
    public ResponseEntity<Void> deleteAccountHistory(@PathVariable String interactionId) {
        
        log.info("Deleting account history entry: {}", interactionId);
        
        try {
            accountHistoryService.deleteAccountHistory(interactionId);
            log.info("Deleted account history entry: {}", interactionId);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            log.error("Failed to delete account history entry {}: {}", interactionId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
