package com.telecom.ocs.provisioning.repositories;

import com.telecom.ocs.provisioning.models.Usage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for Usage entity.
 * 
 * Provides CRUD operations and custom query methods for:
 * - Finding usage records by subscriber (chargedPartyId)
 * - Finding usage records by balance (impactedBalanceId)
 * - Finding usage records by type and record type
 * - Pagination support for list operations
 * - Timestamp-based queries
 * 
 * T179: UsageRepository with findByChargedPartyId
 * T189: Pagination support for subscriber usage queries
 */
@Repository
public interface UsageRepository extends JpaRepository<Usage, String> {

    /**
     * Find all usage records for a specific subscriber
     * FR-095: List usage records for subscriber
     * T179: Basic query method
     * 
     * @param chargedPartyId the subscriber's unique identifier
     * @return List of usage records (may be empty)
     */
    List<Usage> findByChargedPartyId(String chargedPartyId);

    /**
     * Find usage records for a subscriber with pagination
     * FR-095: List usage records with pagination (limit/offset)
     * T189: Paginated query for subscriber usage
     * 
     * @param chargedPartyId the subscriber's unique identifier
     * @param pageable pagination parameters
     * @return Page of usage records
     */
    Page<Usage> findByChargedPartyId(String chargedPartyId, Pageable pageable);

    /**
     * Count usage records for a subscriber
     * 
     * @param chargedPartyId the subscriber's unique identifier
     * @return count of usage records
     */
    long countByChargedPartyId(String chargedPartyId);

    /**
     * Find all usage records impacting a specific balance
     * 
     * @param impactedBalanceId the balance's unique identifier
     * @return List of usage records (may be empty)
     */
    List<Usage> findByImpactedBalanceId(String impactedBalanceId);

    /**
     * Find usage records by usage type
     * 
     * @param usageType the type of usage (VOICE, DATA, SMS, MMS)
     * @return List of usage records (may be empty)
     */
    List<Usage> findByUsageType(Usage.UsageType usageType);

    /**
     * Find usage records by record type
     * 
     * @param recordType the record type (START, INTERIM, STOP, EVENT)
     * @return List of usage records (may be empty)
     */
    List<Usage> findByRecordType(Usage.RecordType recordType);

    /**
     * Find usage records within a timestamp range
     * 
     * @param start the start timestamp (inclusive)
     * @param end the end timestamp (inclusive)
     * @return List of usage records (may be empty)
     */
    List<Usage> findByUsageTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Check if usage record exists by usageId
     * FR-092: Duplicate prevention
     * T185: Used in service layer for duplicate checking
     * 
     * @param usageId the usage ID to check
     * @return true if exists, false otherwise
     */
    boolean existsById(String usageId);
}
