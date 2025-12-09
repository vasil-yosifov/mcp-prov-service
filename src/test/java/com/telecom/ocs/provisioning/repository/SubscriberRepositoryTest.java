package com.telecom.ocs.provisioning.repository;

import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.repositories.SubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for SubscriberRepository.
 * Uses @DataJpaTest for testing data access layer in isolation.
 * 
 * Tests:
 * - T045: Custom query methods (findByMsisdn, findByImsi, findByFirstNameAndLastName)
 * - Basic CRUD operations
 * - Query results validation
 */
@DataJpaTest(excludeAutoConfiguration = FlywayAutoConfiguration.class, properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY)
class SubscriberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubscriberRepository subscriberRepository;

    private Subscriber testSubscriber1;
    private Subscriber testSubscriber2;

    @BeforeEach
    void setUp() {
        // Create test subscriber 1
        testSubscriber1 = new Subscriber();
        testSubscriber1.setSubscriberId(UUID.randomUUID().toString());
        testSubscriber1.setMsisdn("43664123456789");
        testSubscriber1.setImsi("214010123456789");
        testSubscriber1.setFirstName("John");
        testSubscriber1.setLastName("Doe");
        testSubscriber1.setDateOfBirth(LocalDate.of(1990, 1, 15));
        testSubscriber1.setEmail("john.doe@example.com");
        testSubscriber1.setContactNumber("436641234567");
        testSubscriber1.setBillingCycle("1");
        testSubscriber1.setBillingStreet("123 Main St");
        testSubscriber1.setBillingCity("Vienna");
        testSubscriber1.setBillingCountry("Austria");
        testSubscriber1.setState(Subscriber.SubscriberState.PRE_PROVISIONED);
        testSubscriber1.setCreationDate(LocalDateTime.now());
        testSubscriber1.setLastModifiedDate(LocalDateTime.now());
        testSubscriber1.setVersion(0L);

        // Create test subscriber 2
        testSubscriber2 = new Subscriber();
        testSubscriber2.setSubscriberId(UUID.randomUUID().toString());
        testSubscriber2.setMsisdn("43664987654321");
        testSubscriber2.setImsi("214010987654321");
        testSubscriber2.setFirstName("Jane");
        testSubscriber2.setLastName("Smith");
        testSubscriber2.setDateOfBirth(LocalDate.of(1985, 6, 20));
        testSubscriber2.setEmail("jane.smith@example.com");
        testSubscriber2.setState(Subscriber.SubscriberState.ACTIVE);
        testSubscriber2.setCreationDate(LocalDateTime.now());
        testSubscriber2.setLastModifiedDate(LocalDateTime.now());
        testSubscriber2.setVersion(0L);
    }

    /**
     * T045: Test findByMsisdn custom query method
     * Expected: Find subscriber by exact msisdn match
     */
    @Test
    void testFindByMsisdn() {
        // Given
        entityManager.persist(testSubscriber1);
        entityManager.persist(testSubscriber2);
        entityManager.flush();

        // When
        Optional<Subscriber> found = subscriberRepository.findByMsisdn("43664123456789");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getMsisdn()).isEqualTo("43664123456789");
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().getLastName()).isEqualTo("Doe");
    }

    /**
     * T045: Test findByMsisdn returns empty for non-existent msisdn
     * Expected: Empty Optional
     */
    @Test
    void testFindByMsisdnNotFound() {
        // Given
        entityManager.persist(testSubscriber1);
        entityManager.flush();

        // When
        Optional<Subscriber> found = subscriberRepository.findByMsisdn("99999999999999");

        // Then
        assertThat(found).isEmpty();
    }

    /**
     * T045: Test findByImsi custom query method
     * Expected: Find subscriber by exact imsi match
     */
    @Test
    void testFindByImsi() {
        // Given
        entityManager.persist(testSubscriber1);
        entityManager.persist(testSubscriber2);
        entityManager.flush();

        // When
        Optional<Subscriber> found = subscriberRepository.findByImsi("214010123456789");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getImsi()).isEqualTo("214010123456789");
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    /**
     * T045: Test findByImsi returns empty for non-existent imsi
     * Expected: Empty Optional
     */
    @Test
    void testFindByImsiNotFound() {
        // Given
        entityManager.persist(testSubscriber1);
        entityManager.flush();

        // When
        Optional<Subscriber> found = subscriberRepository.findByImsi("999999999999999");

        // Then
        assertThat(found).isEmpty();
    }

    /**
     * T045: Test findByFirstNameAndLastName custom query method
     * Expected: Find subscriber(s) by exact name match
     */
    @Test
    void testFindByFirstNameAndLastName() {
        // Given
        entityManager.persist(testSubscriber1);
        entityManager.persist(testSubscriber2);
        entityManager.flush();

        // When
        List<Subscriber> found = subscriberRepository.findByFirstNameAndLastName("John", "Doe");

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getFirstName()).isEqualTo("John");
        assertThat(found.get(0).getLastName()).isEqualTo("Doe");
        assertThat(found.get(0).getMsisdn()).isEqualTo("43664123456789");
    }

    /**
     * T045: Test findByFirstNameAndLastName with multiple matches
     * Expected: Return all matching subscribers
     */
    @Test
    void testFindByFirstNameAndLastNameMultipleMatches() {
        // Given - Create two subscribers with same name
        testSubscriber2.setFirstName("John");
        testSubscriber2.setLastName("Doe");
        entityManager.persist(testSubscriber1);
        entityManager.persist(testSubscriber2);
        entityManager.flush();

        // When
        List<Subscriber> found = subscriberRepository.findByFirstNameAndLastName("John", "Doe");

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).allMatch(s -> s.getFirstName().equals("John") && s.getLastName().equals("Doe"));
    }

    /**
     * T045: Test findByFirstNameAndLastName returns empty for non-existent name
     * Expected: Empty list
     */
    @Test
    void testFindByFirstNameAndLastNameNotFound() {
        // Given
        entityManager.persist(testSubscriber1);
        entityManager.flush();

        // When
        List<Subscriber> found = subscriberRepository.findByFirstNameAndLastName("NonExistent", "Name");

        // Then
        assertThat(found).isEmpty();
    }

    /**
     * T045: Test basic save operation
     * Expected: Subscriber is persisted with generated ID
     */
    @Test
    void testSaveSubscriber() {
        // When
        Subscriber saved = subscriberRepository.save(testSubscriber1);

        // Then
        assertThat(saved.getSubscriberId()).isNotNull();
        assertThat(saved.getMsisdn()).isEqualTo("43664123456789");
        assertThat(saved.getCreationDate()).isNotNull();
    }

    /**
     * T045: Test findById operation
     * Expected: Find subscriber by primary key
     */
    @Test
    void testFindById() {
        // Given
        entityManager.persist(testSubscriber1);
        entityManager.flush();

        // When
        Optional<Subscriber> found = subscriberRepository.findById(testSubscriber1.getSubscriberId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSubscriberId()).isEqualTo(testSubscriber1.getSubscriberId());
        assertThat(found.get().getMsisdn()).isEqualTo("43664123456789");
    }

    /**
     * T045: Test delete operation
     * Expected: Subscriber is removed from database
     */
    @Test
    void testDeleteSubscriber() {
        // Given
        entityManager.persist(testSubscriber1);
        entityManager.flush();
        String subscriberId = testSubscriber1.getSubscriberId();

        // When
        subscriberRepository.delete(testSubscriber1);
        entityManager.flush();

        // Then
        Optional<Subscriber> found = subscriberRepository.findById(subscriberId);
        assertThat(found).isEmpty();
    }

    /**
     * T045: Test update operation with optimistic locking
     * Expected: Subscriber is updated and version incremented
     */
    @Test
    void testUpdateSubscriber() {
        // Given
        entityManager.persist(testSubscriber1);
        entityManager.flush();
        Long initialVersion = testSubscriber1.getVersion();

        // When
        testSubscriber1.setState(Subscriber.SubscriberState.ACTIVE);
        testSubscriber1.setPreviousState(Subscriber.SubscriberState.PRE_PROVISIONED);
        testSubscriber1.setLastTransitionDate(LocalDateTime.now());
        Subscriber updated = subscriberRepository.save(testSubscriber1);
        entityManager.flush();

        // Then
        assertThat(updated.getState()).isEqualTo(Subscriber.SubscriberState.ACTIVE);
        assertThat(updated.getPreviousState()).isEqualTo(Subscriber.SubscriberState.PRE_PROVISIONED);
        assertThat(updated.getLastTransitionDate()).isNotNull();
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    }

    /**
     * T045: Test findAll operation
     * Expected: Return all subscribers
     */
    @Test
    void testFindAll() {
        // Given
        entityManager.persist(testSubscriber1);
        entityManager.persist(testSubscriber2);
        entityManager.flush();

        // When
        List<Subscriber> all = subscriberRepository.findAll();

        // Then
        assertThat(all).hasSize(2);
        assertThat(all).extracting(Subscriber::getMsisdn)
                .containsExactlyInAnyOrder("43664123456789", "43664987654321");
    }

    /**
     * T045: Test finding subscribers by state
     * Expected: Return subscribers matching the state (assuming this query method exists)
     */
    @Test
    void testFindByState() {
        // Given
        entityManager.persist(testSubscriber1); // PRE_PROVISIONED
        entityManager.persist(testSubscriber2); // ACTIVE
        entityManager.flush();

        // When - assuming findByState method exists in repository
        List<Subscriber> activeSubscribers = subscriberRepository.findByState(Subscriber.SubscriberState.ACTIVE);

        // Then
        assertThat(activeSubscribers).hasSize(1);
        assertThat(activeSubscribers.get(0).getState()).isEqualTo(Subscriber.SubscriberState.ACTIVE);
        assertThat(activeSubscribers.get(0).getFirstName()).isEqualTo("Jane");
    }
}
