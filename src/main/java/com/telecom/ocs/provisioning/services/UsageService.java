package com.telecom.ocs.provisioning.services;

import com.telecom.ocs.provisioning.exceptions.DuplicateResourceException;
import com.telecom.ocs.provisioning.exceptions.ResourceNotFoundException;
import com.telecom.ocs.provisioning.models.Balance;
import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.models.Usage;
import com.telecom.ocs.provisioning.repositories.BalanceRepository;
import com.telecom.ocs.provisioning.repositories.SubscriberRepository;
import com.telecom.ocs.provisioning.repositories.UsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Usage entity business logic.
 * 
 * Implements:
 * - Usage record creation with validation
 * - Automatic balance updates based on balance type
 * - Subscriber and balance validation
 * - Duplicate usage prevention
 * - Pagination support for usage listing
 * - Balance impact auditing
 * 
 * Balance update logic:
 * - FR-096: ALLOWANCE balance deduction (T195)
 * - FR-097: ALLOWANCE floor at 0 (T196)
 * - FR-098: COUNTER balance addition (T197)
 * - T198: Balance value capture (before/after)
 * 
 * T180: UsageService with balance update logic
 * T183: chargedPartyId validation
 * T184: impactedBalanceId validation
 * T185: Duplicate usageId prevention
 * T186: Logging for usage operations
 * T190: listUsageBySubscriberId with pagination
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsageService {

    private final UsageRepository usageRepository;
    private final SubscriberRepository subscriberRepository;
    private final BalanceRepository balanceRepository;

    /**
     * Create a new usage record with automatic balance update.
     * 
     * T180: Core usage creation
     * T183: Subscriber validation
     * T184: Balance validation
     * T185: Duplicate prevention
     * T186: Usage operation logging
     * T195-T198: Balance update logic
     * 
     * @param usage the usage record to create
     * @return the created usage record with balance impact captured
     * @throws ResourceNotFoundException if subscriber or balance not found
     * @throws DuplicateResourceException if usageId already exists
     */
    public Usage createUsage(Usage usage) {
        log.info("Creating usage record: usageId={}, type={}, chargedParty={}, volume={}", 
                usage.getUsageId(), usage.getUsageType(), usage.getChargedPartyId(), usage.getVolumeUsage());

        // T185: Check for duplicate usageId (FR-092)
        if (usageRepository.existsById(usage.getUsageId())) {
            log.warn("Duplicate usage record attempted: usageId={}", usage.getUsageId());
            throw new DuplicateResourceException("Usage record already exists with id: " + usage.getUsageId());
        }

        // T183: Validate subscriber exists (FR-091)
        Subscriber subscriber = subscriberRepository.findById(usage.getChargedPartyId())
                .orElseThrow(() -> {
                    log.warn("Subscriber not found: {}", usage.getChargedPartyId());
                    return new ResourceNotFoundException("Subscriber not found with id: " + usage.getChargedPartyId());
                });

        // T184: Validate balance exists
        Balance balance = balanceRepository.findById(usage.getImpactedBalanceId())
                .orElseThrow(() -> {
                    log.warn("Balance not found: {}", usage.getImpactedBalanceId());
                    return new ResourceNotFoundException("Balance not found with id: " + usage.getImpactedBalanceId());
                });

        // T198: Capture balance value before update
        Long balanceValueBefore = balance.getBalanceAvailable();
        usage.setBalanceValueBefore(balanceValueBefore);

        // Apply balance update based on balance type
        Long balanceValueAfter = updateBalance(balance, usage.getVolumeUsage());
        usage.setBalanceValueAfter(balanceValueAfter);

        // Save updated balance
        balanceRepository.save(balance);
        log.info("Updated balance: balanceId={}, type={}, before={}, after={}", 
                balance.getBalanceId(), balance.getBalanceType(), balanceValueBefore, balanceValueAfter);

        // Set metadata if not provided
        if (usage.getUsageTimestamp() == null) {
            usage.setUsageTimestamp(LocalDateTime.now());
        }
        if (usage.getCreationDate() == null) {
            usage.setCreationDate(LocalDateTime.now());
        }

        // Save usage record
        Usage saved = usageRepository.save(usage);
        log.info("Created usage record: usageId={}, balanceImpact={} (before={}, after={})", 
                saved.getUsageId(), balanceValueBefore - balanceValueAfter, balanceValueBefore, balanceValueAfter);

        return saved;
    }

    /**
     * Update balance based on balance type and volume usage.
     * 
     * T195: ALLOWANCE balance deduction (FR-096)
     * T196: ALLOWANCE floor at 0 (FR-097)
     * T197: COUNTER balance addition (FR-098)
     * 
     * @param balance the balance to update
     * @param volumeUsage the volume consumed/counted
     * @return the new balance available value
     */
    private Long updateBalance(Balance balance, Long volumeUsage) {
        Long currentAvailable = balance.getBalanceAvailable();
        Long newAvailable;

        if (balance.getBalanceType() == Balance.BalanceType.ALLOWANCE) {
            // T195: Deduct volumeUsage from balanceAvailable (FR-096)
            newAvailable = currentAvailable - volumeUsage;
            
            // T196: Floor at 0 - prevent negative balance (FR-097)
            if (newAvailable < 0) {
                log.warn("Volume usage {} exceeds available balance {} for ALLOWANCE balance {}, setting to 0", 
                        volumeUsage, currentAvailable, balance.getBalanceId());
                newAvailable = 0L;
            }
            
            log.debug("ALLOWANCE balance deduction: {} - {} = {}", currentAvailable, volumeUsage, newAvailable);
            
        } else if (balance.getBalanceType() == Balance.BalanceType.COUNTER) {
            // T197: Add volumeUsage to balanceAvailable (FR-098)
            newAvailable = currentAvailable + volumeUsage;
            log.debug("COUNTER balance addition: {} + {} = {}", currentAvailable, volumeUsage, newAvailable);
            
        } else {
            // Unexpected balance type - should not happen with enum
            log.error("Unexpected balance type: {}", balance.getBalanceType());
            throw new IllegalStateException("Unexpected balance type: " + balance.getBalanceType());
        }

        balance.setBalanceAvailable(newAvailable);
        balance.setLastModifiedDate(LocalDateTime.now());
        
        return newAvailable;
    }

    /**
     * Get usage record by ID.
     * 
     * @param usageId the usage ID
     * @return the usage record
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Usage getUsageById(String usageId) {
        log.debug("Retrieving usage record: {}", usageId);
        
        return usageRepository.findById(usageId)
                .orElseThrow(() -> {
                    log.warn("Usage record not found: {}", usageId);
                    return new ResourceNotFoundException("Usage record not found with id: " + usageId);
                });
    }

    /**
     * List usage records for a subscriber with pagination.
     * 
     * FR-095: List usage for subscriber with pagination
     * T190: Paginated listing implementation
     * 
     * @param subscriberId the subscriber ID
     * @param pageable pagination parameters
     * @return page of usage records
     * @throws ResourceNotFoundException if subscriber not found
     */
    @Transactional(readOnly = true)
    public Page<Usage> listUsageBySubscriberId(String subscriberId, Pageable pageable) {
        log.debug("Listing usage for subscriber: {} with pagination: {}", subscriberId, pageable);
        
        // Validate subscriber exists
        subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> {
                    log.warn("Subscriber not found: {}", subscriberId);
                    return new ResourceNotFoundException("Subscriber not found with id: " + subscriberId);
                });
        
        Page<Usage> usagePage = usageRepository.findByChargedPartyId(subscriberId, pageable);
        log.debug("Found {} usage records for subscriber: {}", usagePage.getTotalElements(), subscriberId);
        
        return usagePage;
    }

    /**
     * List all usage records for a subscriber (non-paginated).
     * 
     * @param subscriberId the subscriber ID
     * @return list of usage records
     */
    @Transactional(readOnly = true)
    public List<Usage> listAllUsageBySubscriberId(String subscriberId) {
        log.debug("Listing all usage for subscriber: {}", subscriberId);
        
        // Validate subscriber exists
        subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> {
                    log.warn("Subscriber not found: {}", subscriberId);
                    return new ResourceNotFoundException("Subscriber not found with id: " + subscriberId);
                });
        
        List<Usage> usageList = usageRepository.findByChargedPartyId(subscriberId);
        log.debug("Found {} usage records for subscriber: {}", usageList.size(), subscriberId);
        
        return usageList;
    }

    /**
     * List usage records by balance.
     * 
     * @param balanceId the balance ID
     * @return list of usage records
     */
    @Transactional(readOnly = true)
    public List<Usage> listUsageByBalanceId(String balanceId) {
        log.debug("Listing usage for balance: {}", balanceId);
        return usageRepository.findByImpactedBalanceId(balanceId);
    }

    /**
     * Count usage records for a subscriber.
     * 
     * @param subscriberId the subscriber ID
     * @return count of usage records
     */
    @Transactional(readOnly = true)
    public long countUsageBySubscriberId(String subscriberId) {
        return usageRepository.countByChargedPartyId(subscriberId);
    }
}
