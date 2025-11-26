package com.telecom.ocs.provisioning.repositories;

import com.telecom.ocs.provisioning.models.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Subscriber entity.
 * 
 * Provides CRUD operations and custom query methods for:
 * - Finding by msisdn (phone number lookup)
 * - Finding by imsi (SIM card lookup)
 * - Finding by first and last name (customer search)
 * - Finding by state (lifecycle filtering)
 * 
 * Custom methods support T041 (subscriber lookup endpoints)
 */
@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, String> {

    /**
     * Find subscriber by msisdn (international phone number)
     * 
     * @param msisdn the international phone number (14-15 digits)
     * @return Optional containing subscriber if found
     */
    Optional<Subscriber> findByMsisdn(String msisdn);

    /**
     * Find subscriber by imsi (International Mobile Subscriber Identity)
     * 
     * @param imsi the IMSI (15 digits)
     * @return Optional containing subscriber if found
     */
    Optional<Subscriber> findByImsi(String imsi);

    /**
     * Find subscribers by first and last name
     * 
     * @param firstName the first name
     * @param lastName the last name
     * @return List of matching subscribers (may be empty)
     */
    List<Subscriber> findByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Find all subscribers in a specific lifecycle state
     * 
     * @param state the lifecycle state
     * @return List of matching subscribers (may be empty)
     */
    List<Subscriber> findByState(Subscriber.SubscriberState state);

    /**
     * Check if a subscriber with given msisdn exists (excluding deactivated state)
     * Used for FR-007 uniqueness validation
     * 
     * @param msisdn the msisdn to check
     * @return true if exists and not deactivated
     */
    @Query("SELECT COUNT(s) > 0 FROM Subscriber s WHERE s.msisdn = :msisdn AND s.state != 'DEACTIVATED'")
    boolean existsByMsisdnAndNotDeactivated(@Param("msisdn") String msisdn);
}
