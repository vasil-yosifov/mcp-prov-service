package com.telecom.ocs.provisioning.controllers;

import com.telecom.ocs.provisioning.api.model.Balance;
import com.telecom.ocs.provisioning.mappers.BalanceMapper;
import com.telecom.ocs.provisioning.services.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for Balance-related API endpoints.
 * 
 * Provides balance operations for subscriptions:
 * - Create balances for a subscription
 * - List balances for a subscription
 * - Delete all balances for a subscription
 * - Balance deduction/refund operations
 * - Rollover processing
 * 
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>HTTP request/response handling</li>
 *   <li>Entity ↔ DTO mapping via BalanceMapper</li>
 *   <li>Business logic delegation to BalanceService</li>
 *   <li>Request/response logging</li>
 * </ul>
 * 
 * @see BalanceService Business logic layer
 * @see BalanceMapper Entity/DTO conversion
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping
public class BalanceController {

    private final BalanceService balanceService;
    private final BalanceMapper balanceMapper;

    /**
     * Create a new balance for a subscription.
     * 
     * POST /subscriptions/{subscriptionId}/balances
     * 
     * @param subscriptionId The ID of the subscription
     * @param balance Balance details to create
     * @return 201 Created with the created balance
     */
    @PostMapping("/subscriptions/{subscriptionId}/balances")
    public ResponseEntity<Balance> createBalance(
            @PathVariable String subscriptionId, 
            @Valid @RequestBody Balance balance) {
        
        log.info("Creating balance for subscription: {}", subscriptionId);
        
        // Ensure subscriptionId matches
        balance.setSubscriptionId(subscriptionId);
        
        // Convert DTO to entity
        com.telecom.ocs.provisioning.models.Balance entity = balanceMapper.toEntity(balance);
        
        // Save via service
        com.telecom.ocs.provisioning.models.Balance created = balanceService.createBalance(subscriptionId, entity);
        
        // Convert back to DTO
        Balance response = balanceMapper.toDto(created);
        
        log.info("Created balance with ID: {}", response.getBalanceId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all balances for a subscription.
     * 
     * GET /subscriptions/{subscriptionId}/balances
     * 
     * @param subscriptionId The subscription ID
     * @param activeOnly Optional filter for active balances only (non-expired)
     * @param groupOnly Optional filter for group-level balances only
     * @return 200 OK with list of balances (may be empty)
     */
    @GetMapping("/subscriptions/{subscriptionId}/balances")
    public ResponseEntity<List<Balance>> getBalancesBySubscriptionId(
            @PathVariable String subscriptionId,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Boolean groupOnly) {
        
        log.info("Fetching balances for subscription: {} (activeOnly={}, groupOnly={})", 
                 subscriptionId, activeOnly, groupOnly);
        
        List<com.telecom.ocs.provisioning.models.Balance> entities;
        
        // Apply filters based on query parameters
        if (Boolean.TRUE.equals(groupOnly)) {
            entities = balanceService.getGroupBalances(subscriptionId);
            log.info("Retrieved {} group balances", entities.size());
        } else if (Boolean.TRUE.equals(activeOnly)) {
            entities = balanceService.getActiveBalances(subscriptionId);
            log.info("Retrieved {} active balances", entities.size());
        } else {
            entities = balanceService.getBalancesBySubscriptionId(subscriptionId);
            log.info("Retrieved {} total balances", entities.size());
        }
        
        List<Balance> response = entities.stream()
                .map(balanceMapper::toDto)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete all balances for a subscription.
     * 
     * DELETE /subscriptions/{subscriptionId}/balances
     * 
     * @param subscriptionId The subscription ID
     * @return 204 No Content on successful deletion
     */
    @DeleteMapping("/subscriptions/{subscriptionId}/balances")
    public ResponseEntity<Void> deleteBalances(@PathVariable String subscriptionId) {
        log.info("Deleting all balances for subscription: {}", subscriptionId);
        
        List<com.telecom.ocs.provisioning.models.Balance> balances = 
                balanceService.getBalancesBySubscriptionId(subscriptionId);
        
        for (com.telecom.ocs.provisioning.models.Balance balance : balances) {
            balanceService.deleteBalance(balance.getBalanceId());
        }
        
        log.info("Deleted {} balances for subscription: {}", balances.size(), subscriptionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get a balance by ID.
     * 
     * GET /balances/{balanceId}
     * 
     * @param balanceId The balance ID
     * @return 200 OK with balance, or 404 Not Found
     */
    @GetMapping("/balances/{balanceId}")
    public ResponseEntity<Balance> getBalanceById(@PathVariable String balanceId) {
        log.info("Fetching balance: {}", balanceId);
        
        com.telecom.ocs.provisioning.models.Balance entity = balanceService.getBalanceById(balanceId);
        Balance response = balanceMapper.toDto(entity);
        
        log.info("Retrieved balance: {}", balanceId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing balance.
     * 
     * PATCH /balances/{balanceId}
     * 
     * @param balanceId The balance ID
     * @param balance Updated balance fields
     * @return 200 OK with updated balance
     */
    @PatchMapping("/balances/{balanceId}")
    public ResponseEntity<Balance> updateBalance(
            @PathVariable String balanceId, 
            @Valid @RequestBody Balance balance) {
        
        log.info("Updating balance: {}", balanceId);
        
        // Ensure ID matches
        balance.setBalanceId(balanceId);
        
        // Convert DTO to entity
        com.telecom.ocs.provisioning.models.Balance entity = balanceMapper.toEntity(balance);
        
        // Update via service
        com.telecom.ocs.provisioning.models.Balance updated = balanceService.updateBalance(balanceId, entity);
        
        // Convert back to DTO
        Balance response = balanceMapper.toDto(updated);
        
        log.info("Updated balance: {}", balanceId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a balance.
     * 
     * DELETE /balances/{balanceId}
     * 
     * @param balanceId The balance ID
     * @return 204 No Content on successful deletion
     */
    @DeleteMapping("/balances/{balanceId}")
    public ResponseEntity<Void> deleteBalance(@PathVariable String balanceId) {
        log.info("Deleting balance: {}", balanceId);
        
        balanceService.deleteBalance(balanceId);
        
        log.info("Deleted balance: {}", balanceId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deduct an amount from a balance.
     * 
     * POST /balances/{balanceId}/deduct
     * 
     * @param balanceId The balance ID
     * @param amount Amount to deduct
     * @return 200 OK with updated balance
     */
    @PostMapping("/balances/{balanceId}/deduct")
    public ResponseEntity<Balance> deductBalance(
            @PathVariable String balanceId, 
            @RequestParam Long amount) {
        
        log.info("Deducting {} from balance: {}", amount, balanceId);
        
        com.telecom.ocs.provisioning.models.Balance updated = balanceService.deductBalance(balanceId, amount);
        Balance response = balanceMapper.toDto(updated);
        
        log.info("Deducted {} from balance: {}, new available: {}", 
                 amount, balanceId, response.getBalanceAvailable());
        return ResponseEntity.ok(response);
    }

    /**
     * Refund an amount to a balance.
     * 
     * POST /balances/{balanceId}/refund
     * 
     * @param balanceId The balance ID
     * @param amount Amount to refund
     * @return 200 OK with updated balance
     */
    @PostMapping("/balances/{balanceId}/refund")
    public ResponseEntity<Balance> refundBalance(
            @PathVariable String balanceId, 
            @RequestParam Long amount) {
        
        log.info("Refunding {} to balance: {}", amount, balanceId);
        
        com.telecom.ocs.provisioning.models.Balance updated = balanceService.refundBalance(balanceId, amount);
        Balance response = balanceMapper.toDto(updated);
        
        log.info("Refunded {} to balance: {}, new available: {}", 
                 amount, balanceId, response.getBalanceAvailable());
        return ResponseEntity.ok(response);
    }

    /**
     * Process rollover for a balance at billing cycle renewal.
     * 
     * POST /balances/{balanceId}/rollover
     * 
     * @param balanceId The balance ID
     * @return 200 OK with updated balance after rollover processing
     */
    @PostMapping("/balances/{balanceId}/rollover")
    public ResponseEntity<Balance> processRollover(@PathVariable String balanceId) {
        log.info("Processing rollover for balance: {}", balanceId);
        
        com.telecom.ocs.provisioning.models.Balance updated = balanceService.processRollover(balanceId);
        Balance response = balanceMapper.toDto(updated);
        
        log.info("Processed rollover for balance: {}, rollover amount: {}", 
                 balanceId, response.getRolloverAmount());
        return ResponseEntity.ok(response);
    }
}
