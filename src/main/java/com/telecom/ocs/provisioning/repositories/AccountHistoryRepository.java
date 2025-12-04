package com.telecom.ocs.provisioning.repositories;

import com.telecom.ocs.provisioning.models.AccountHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AccountHistory entity.
 * 
 * T095: Custom query methods for chronological ordering and entity filtering
 */
@Repository
public interface AccountHistoryRepository extends JpaRepository<AccountHistory, String> {

    /**
     * Find all account history entries for a given entity, ordered by startDateTime descending (newest first).
     * If startDateTime is null, falls back to creationDate for ordering.
     * 
     * T095: Chronological ordering support for audit trail queries
     * 
     * @param entityId the entity ID to filter by
     * @return list of account history entries in reverse chronological order
     */
    @Query("SELECT ah FROM AccountHistory ah WHERE ah.entityId = :entityId " +
           "ORDER BY COALESCE(ah.startDateTime, ah.creationDate) DESC, ah.creationDate DESC")
    List<AccountHistory> findByEntityIdOrderByStartDateTimeDesc(@Param("entityId") String entityId);
}
