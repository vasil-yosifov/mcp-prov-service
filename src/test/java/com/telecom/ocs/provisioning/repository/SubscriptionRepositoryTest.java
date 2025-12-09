package com.telecom.ocs.provisioning.repository;

import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.models.Subscription;
import com.telecom.ocs.provisioning.repositories.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for SubscriptionRepository.
 * Uses @DataJpaTest for testing data access layer in isolation.
 * 
 * Tests:
 * - T061: Custom query methods (findBySubscriberId, findByState)
 * - Basic CRUD operations
 * - Query results validation
 * - Relationship with Subscriber entity
 */
@DataJpaTest(excludeAutoConfiguration = FlywayAutoConfiguration.class, properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY)
class SubscriptionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private Subscriber testSubscriber;
    private Subscription testSubscription1;
    private Subscription testSubscription2;

    @BeforeEach
    void setUp() {
        // Create test subscriber first
        testSubscriber = new Subscriber();
        testSubscriber.setSubscriberId(UUID.randomUUID().toString());
        testSubscriber.setMsisdn("43664123456789");
        testSubscriber.setImsi("214010123456789");
        testSubscriber.setFirstName("John");
        testSubscriber.setLastName("Doe");
        testSubscriber.setState(Subscriber.SubscriberState.ACTIVE);
        testSubscriber.setCreationDate(LocalDateTime.now());
        testSubscriber.setLastModifiedDate(LocalDateTime.now());
        testSubscriber.setVersion(0L);

        // Create test subscription 1
        testSubscription1 = new Subscription();
        testSubscription1.setSubscriptionId(UUID.randomUUID().toString());
        testSubscription1.setSubscriberId(testSubscriber.getSubscriberId());
        testSubscription1.setOfferId("OFFER-001");
        testSubscription1.setOfferName("Premium Data Plan");
        testSubscription1.setSubscriptionType("DATA");
        testSubscription1.setState(Subscription.SubscriptionState.PENDING);
        testSubscription1.setRecurring(true);
        testSubscription1.setMaxRecurringCycles(12);
        testSubscription1.setRecurringCyclesCompleted(0);
        testSubscription1.setCycleLengthUnits(1);
        testSubscription1.setCycleLengthType(Subscription.CycleLengthType.MONTHS);
        testSubscription1.setCreationDate(LocalDateTime.now());
        testSubscription1.setLastModifiedDate(LocalDateTime.now());
        testSubscription1.setVersion(0L);

        // Create test subscription 2
        testSubscription2 = new Subscription();
        testSubscription2.setSubscriptionId(UUID.randomUUID().toString());
        testSubscription2.setSubscriberId(testSubscriber.getSubscriberId());
        testSubscription2.setOfferId("OFFER-002");
        testSubscription2.setOfferName("Voice Plan");
        testSubscription2.setSubscriptionType("VOICE");
        testSubscription2.setState(Subscription.SubscriptionState.ACTIVE);
        testSubscription2.setRecurring(false);
        testSubscription2.setRecurringCyclesCompleted(0);
        testSubscription2.setCreationDate(LocalDateTime.now());
        testSubscription2.setLastModifiedDate(LocalDateTime.now());
        testSubscription2.setVersion(0L);
    }

    // =========================================================================
    // Basic CRUD Operations
    // =========================================================================

    /**
     * T061: Test basic save operation
     * Expected: Subscription is persisted with generated ID
     */
    @Test
    void testSaveSubscription() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.flush();

        // When
        Subscription saved = subscriptionRepository.save(testSubscription1);

        // Then
        assertThat(saved.getSubscriptionId()).isNotNull();
        assertThat(saved.getOfferId()).isEqualTo("OFFER-001");
        assertThat(saved.getOfferName()).isEqualTo("Premium Data Plan");
        assertThat(saved.getState()).isEqualTo(Subscription.SubscriptionState.PENDING);
        assertThat(saved.getCreationDate()).isNotNull();
    }

    /**
     * T061: Test findById operation
     * Expected: Find subscription by primary key
     */
    @Test
    void testFindById() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1);
        entityManager.flush();

        // When
        Optional<Subscription> found = subscriptionRepository.findById(testSubscription1.getSubscriptionId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSubscriptionId()).isEqualTo(testSubscription1.getSubscriptionId());
        assertThat(found.get().getOfferId()).isEqualTo("OFFER-001");
    }

    /**
     * T061: Test findById returns empty for non-existent subscription
     * Expected: Empty Optional
     */
    @Test
    void testFindByIdNotFound() {
        // When
        Optional<Subscription> found = subscriptionRepository.findById("non-existent-id");

        // Then
        assertThat(found).isEmpty();
    }

    /**
     * T061: Test delete operation
     * Expected: Subscription is removed from database
     */
    @Test
    void testDeleteSubscription() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1);
        entityManager.flush();
        String subscriptionId = testSubscription1.getSubscriptionId();

        // When
        subscriptionRepository.delete(testSubscription1);
        entityManager.flush();

        // Then
        Optional<Subscription> found = subscriptionRepository.findById(subscriptionId);
        assertThat(found).isEmpty();
    }

    /**
     * T061: Test update operation with optimistic locking
     * Expected: Subscription is updated and version incremented
     */
    @Test
    void testUpdateSubscription() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1);
        entityManager.flush();
        Long initialVersion = testSubscription1.getVersion();

        // When
        testSubscription1.setState(Subscription.SubscriptionState.ACTIVE);
        testSubscription1.setActivationDate(LocalDateTime.now());
        Subscription updated = subscriptionRepository.save(testSubscription1);
        entityManager.flush();

        // Then
        assertThat(updated.getState()).isEqualTo(Subscription.SubscriptionState.ACTIVE);
        assertThat(updated.getActivationDate()).isNotNull();
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    }

    // =========================================================================
    // Custom Query Methods
    // =========================================================================

    /**
     * T061: Test findBySubscriberId custom query method
     * Expected: Find all subscriptions for a specific subscriber
     */
    @Test
    void testFindBySubscriberId() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1);
        entityManager.persist(testSubscription2);
        entityManager.flush();

        // When
        List<Subscription> found = subscriptionRepository.findBySubscriberId(testSubscriber.getSubscriberId());

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Subscription::getSubscriberId)
                .containsOnly(testSubscriber.getSubscriberId());
        assertThat(found).extracting(Subscription::getOfferId)
                .containsExactlyInAnyOrder("OFFER-001", "OFFER-002");
    }

    /**
     * T061: Test findBySubscriberId returns empty list for subscriber with no subscriptions
     * Expected: Empty list
     */
    @Test
    void testFindBySubscriberIdNoSubscriptions() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.flush();

        // When
        List<Subscription> found = subscriptionRepository.findBySubscriberId(testSubscriber.getSubscriberId());

        // Then
        assertThat(found).isEmpty();
    }

    /**
     * T061: Test findBySubscriberId returns empty for non-existent subscriber
     * Expected: Empty list
     */
    @Test
    void testFindBySubscriberIdNotFound() {
        // When
        List<Subscription> found = subscriptionRepository.findBySubscriberId("non-existent-subscriber-id");

        // Then
        assertThat(found).isEmpty();
    }

    /**
     * T061: Test findByState custom query method
     * Expected: Find all subscriptions in a specific state
     */
    @Test
    void testFindByState() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1); // PENDING
        entityManager.persist(testSubscription2); // ACTIVE
        entityManager.flush();

        // When
        List<Subscription> pendingSubscriptions = subscriptionRepository.findByState(Subscription.SubscriptionState.PENDING);
        List<Subscription> activeSubscriptions = subscriptionRepository.findByState(Subscription.SubscriptionState.ACTIVE);

        // Then
        assertThat(pendingSubscriptions).hasSize(1);
        assertThat(pendingSubscriptions.get(0).getState()).isEqualTo(Subscription.SubscriptionState.PENDING);
        assertThat(pendingSubscriptions.get(0).getOfferId()).isEqualTo("OFFER-001");

        assertThat(activeSubscriptions).hasSize(1);
        assertThat(activeSubscriptions.get(0).getState()).isEqualTo(Subscription.SubscriptionState.ACTIVE);
        assertThat(activeSubscriptions.get(0).getOfferId()).isEqualTo("OFFER-002");
    }

    /**
     * T061: Test findByState returns empty for state with no subscriptions
     * Expected: Empty list
     */
    @Test
    void testFindByStateNoResults() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1); // PENDING
        entityManager.flush();

        // When
        List<Subscription> expiredSubscriptions = subscriptionRepository.findByState(Subscription.SubscriptionState.EXPIRED);

        // Then
        assertThat(expiredSubscriptions).isEmpty();
    }

    /**
     * T061: Test findBySubscriberIdAndState combined query
     * Expected: Find subscriptions for specific subscriber in specific state
     */
    @Test
    void testFindBySubscriberIdAndState() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1); // PENDING
        entityManager.persist(testSubscription2); // ACTIVE
        entityManager.flush();

        // When
        List<Subscription> found = subscriptionRepository.findBySubscriberIdAndState(
                testSubscriber.getSubscriberId(),
                Subscription.SubscriptionState.ACTIVE
        );

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getState()).isEqualTo(Subscription.SubscriptionState.ACTIVE);
        assertThat(found.get(0).getOfferId()).isEqualTo("OFFER-002");
    }

    // =========================================================================
    // Recurring Subscription Queries
    // =========================================================================

    /**
     * T061: Test finding recurring subscriptions
     * Expected: Find only subscriptions with recurring = true
     */
    @Test
    void testFindRecurringSubscriptions() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1); // recurring = true
        entityManager.persist(testSubscription2); // recurring = false
        entityManager.flush();

        // When
        List<Subscription> recurringSubscriptions = subscriptionRepository.findByRecurring(true);
        List<Subscription> nonRecurringSubscriptions = subscriptionRepository.findByRecurring(false);

        // Then
        assertThat(recurringSubscriptions).hasSize(1);
        assertThat(recurringSubscriptions.get(0).getRecurring()).isTrue();
        assertThat(recurringSubscriptions.get(0).getOfferId()).isEqualTo("OFFER-001");

        assertThat(nonRecurringSubscriptions).hasSize(1);
        assertThat(nonRecurringSubscriptions.get(0).getRecurring()).isFalse();
        assertThat(nonRecurringSubscriptions.get(0).getOfferId()).isEqualTo("OFFER-002");
    }

    /**
     * T061: Test findByOfferId query
     * Expected: Find subscriptions with specific offer ID
     */
    @Test
    void testFindByOfferId() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1);
        entityManager.persist(testSubscription2);
        entityManager.flush();

        // When
        List<Subscription> found = subscriptionRepository.findByOfferId("OFFER-001");

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOfferId()).isEqualTo("OFFER-001");
        assertThat(found.get(0).getOfferName()).isEqualTo("Premium Data Plan");
    }

    // =========================================================================
    // Count and Exists Queries
    // =========================================================================

    /**
     * T061: Test count subscriptions for subscriber
     * Expected: Return correct count
     */
    @Test
    void testCountBySubscriberId() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1);
        entityManager.persist(testSubscription2);
        entityManager.flush();

        // When
        long count = subscriptionRepository.countBySubscriberId(testSubscriber.getSubscriberId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    /**
     * T061: Test exists by subscriptionId
     * Expected: Return true for existing, false for non-existing
     */
    @Test
    void testExistsBySubscriptionId() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1);
        entityManager.flush();

        // When
        boolean exists = subscriptionRepository.existsById(testSubscription1.getSubscriptionId());
        boolean notExists = subscriptionRepository.existsById("non-existent-id");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    // =========================================================================
    // Expiration and Lifecycle Queries
    // =========================================================================

    /**
     * T061: Test finding subscriptions ready for expiration
     * Expected: Find subscriptions where recurringCyclesCompleted >= maxRecurringCycles
     */
    @Test
    void testFindSubscriptionsReadyForExpiration() {
        // Given
        entityManager.persist(testSubscriber);
        
        // Subscription that should expire
        testSubscription1.setRecurringCyclesCompleted(12);
        testSubscription1.setMaxRecurringCycles(12);
        testSubscription1.setState(Subscription.SubscriptionState.ACTIVE);
        entityManager.persist(testSubscription1);
        
        // Subscription that should not expire
        Subscription notReady = new Subscription();
        notReady.setSubscriptionId(UUID.randomUUID().toString());
        notReady.setSubscriberId(testSubscriber.getSubscriberId());
        notReady.setOfferId("OFFER-003");
        notReady.setOfferName("Long Term Plan");
        notReady.setState(Subscription.SubscriptionState.ACTIVE);
        notReady.setRecurring(true);
        notReady.setMaxRecurringCycles(24);
        notReady.setRecurringCyclesCompleted(10);
        notReady.setCreationDate(LocalDateTime.now());
        notReady.setLastModifiedDate(LocalDateTime.now());
        notReady.setVersion(0L);
        entityManager.persist(notReady);
        
        entityManager.flush();

        // When
        List<Subscription> readyForExpiration = subscriptionRepository.findReadyForExpiration();

        // Then
        assertThat(readyForExpiration).hasSize(1);
        assertThat(readyForExpiration.get(0).getSubscriptionId()).isEqualTo(testSubscription1.getSubscriptionId());
        assertThat(readyForExpiration.get(0).getRecurringCyclesCompleted())
                .isGreaterThanOrEqualTo(readyForExpiration.get(0).getMaxRecurringCycles());
    }

    /**
     * T061: Test finding active subscriptions for a subscriber
     * Expected: Find only ACTIVE subscriptions
     */
    @Test
    void testFindActiveSubscriptionsBySubscriberId() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1); // PENDING
        entityManager.persist(testSubscription2); // ACTIVE
        
        // Add expired subscription
        Subscription expired = new Subscription();
        expired.setSubscriptionId(UUID.randomUUID().toString());
        expired.setSubscriberId(testSubscriber.getSubscriberId());
        expired.setOfferId("OFFER-EXPIRED");
        expired.setOfferName("Expired Plan");
        expired.setState(Subscription.SubscriptionState.EXPIRED);
        expired.setRecurring(false);
        expired.setCreationDate(LocalDateTime.now());
        expired.setLastModifiedDate(LocalDateTime.now());
        expired.setVersion(0L);
        entityManager.persist(expired);
        
        entityManager.flush();

        // When
        List<Subscription> activeSubscriptions = subscriptionRepository.findBySubscriberIdAndState(
                testSubscriber.getSubscriberId(),
                Subscription.SubscriptionState.ACTIVE
        );

        // Then
        assertThat(activeSubscriptions).hasSize(1);
        assertThat(activeSubscriptions.get(0).getState()).isEqualTo(Subscription.SubscriptionState.ACTIVE);
        assertThat(activeSubscriptions.get(0).getOfferId()).isEqualTo("OFFER-002");
    }

    // =========================================================================
    // findAll and Pagination
    // =========================================================================

    /**
     * T061: Test findAll operation
     * Expected: Return all subscriptions
     */
    @Test
    void testFindAll() {
        // Given
        entityManager.persist(testSubscriber);
        entityManager.persist(testSubscription1);
        entityManager.persist(testSubscription2);
        entityManager.flush();

        // When
        List<Subscription> all = subscriptionRepository.findAll();

        // Then
        assertThat(all).hasSize(2);
        assertThat(all).extracting(Subscription::getOfferId)
                .containsExactlyInAnyOrder("OFFER-001", "OFFER-002");
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    /**
     * T061: Test subscription with null optional fields
     * Expected: Subscription is persisted with null values
     */
    @Test
    void testSubscriptionWithNullFields() {
        // Given
        entityManager.persist(testSubscriber);
        
        Subscription minimal = new Subscription();
        minimal.setSubscriptionId(UUID.randomUUID().toString());
        minimal.setSubscriberId(testSubscriber.getSubscriberId());
        minimal.setOfferId("OFFER-MINIMAL");
        minimal.setState(Subscription.SubscriptionState.PENDING);
        minimal.setCreationDate(LocalDateTime.now());
        minimal.setLastModifiedDate(LocalDateTime.now());
        minimal.setVersion(0L);
        // Leave offerName, subscriptionType, recurring, etc. as null

        // When
        Subscription saved = subscriptionRepository.save(minimal);
        entityManager.flush();

        Optional<Subscription> found = subscriptionRepository.findById(saved.getSubscriptionId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getOfferName()).isNull();
        assertThat(found.get().getSubscriptionType()).isNull();
    }

    /**
     * T061: Test multiple subscribers with multiple subscriptions
     * Expected: Correct subscriptions returned for each subscriber
     */
    @Test
    void testMultipleSubscribersWithMultipleSubscriptions() {
        // Given - Create second subscriber
        Subscriber subscriber2 = new Subscriber();
        subscriber2.setSubscriberId(UUID.randomUUID().toString());
        subscriber2.setMsisdn("43664987654321");
        subscriber2.setState(Subscriber.SubscriberState.ACTIVE);
        subscriber2.setCreationDate(LocalDateTime.now());
        subscriber2.setLastModifiedDate(LocalDateTime.now());
        subscriber2.setVersion(0L);

        entityManager.persist(testSubscriber);
        entityManager.persist(subscriber2);

        // Create subscriptions for each subscriber
        entityManager.persist(testSubscription1); // For testSubscriber
        entityManager.persist(testSubscription2); // For testSubscriber

        Subscription sub3 = new Subscription();
        sub3.setSubscriptionId(UUID.randomUUID().toString());
        sub3.setSubscriberId(subscriber2.getSubscriberId());
        sub3.setOfferId("OFFER-003");
        sub3.setOfferName("Basic Plan");
        sub3.setState(Subscription.SubscriptionState.ACTIVE);
        sub3.setCreationDate(LocalDateTime.now());
        sub3.setLastModifiedDate(LocalDateTime.now());
        sub3.setVersion(0L);
        entityManager.persist(sub3);

        entityManager.flush();

        // When
        List<Subscription> subscriber1Subs = subscriptionRepository.findBySubscriberId(testSubscriber.getSubscriberId());
        List<Subscription> subscriber2Subs = subscriptionRepository.findBySubscriberId(subscriber2.getSubscriberId());

        // Then
        assertThat(subscriber1Subs).hasSize(2);
        assertThat(subscriber1Subs).extracting(Subscription::getOfferId)
                .containsExactlyInAnyOrder("OFFER-001", "OFFER-002");

        assertThat(subscriber2Subs).hasSize(1);
        assertThat(subscriber2Subs.get(0).getOfferId()).isEqualTo("OFFER-003");
    }
}
