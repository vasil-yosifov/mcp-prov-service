package com.telecom.ocs.provisioning.repository;

import com.telecom.ocs.provisioning.models.Balance;
import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.models.Subscription;
import com.telecom.ocs.provisioning.repositories.BalanceRepository;
import com.telecom.ocs.provisioning.repositories.SubscriberRepository;
import com.telecom.ocs.provisioning.repositories.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T077: Repository tests for BalanceRepository.
 * Tests data access layer operations for Balance entity.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BalanceRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("ocs_provisioning_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Subscriber testSubscriber;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        // Create test subscriber
        testSubscriber = new Subscriber();
        testSubscriber.setSubscriberId(UUID.randomUUID().toString());
        testSubscriber.setMsisdn("436641234567");
        testSubscriber.setFirstName("Test");
        testSubscriber.setLastName("Subscriber");
        testSubscriber.setCreationDate(LocalDateTime.now());
        testSubscriber.setLastModifiedDate(LocalDateTime.now());
        testSubscriber = subscriberRepository.save(testSubscriber);

        // Create test subscription
        testSubscription = new Subscription();
        testSubscription.setSubscriptionId(UUID.randomUUID().toString());
        testSubscription.setSubscriberId(testSubscriber.getSubscriberId()); // Set subscriberId for validation
        testSubscription.setSubscriber(testSubscriber);
        testSubscription.setOfferId("DATA-PLAN-001");
        testSubscription.setOfferName("10GB Monthly Plan");
        testSubscription.setSubscriptionType("DATA");
        testSubscription.setState(Subscription.SubscriptionState.ACTIVE);
        testSubscription.setRecurring(true);
        testSubscription.setMaxRecurringCycles(12);
        testSubscription.setCycleLengthUnits(1);
        testSubscription.setCycleLengthType(Subscription.CycleLengthType.MONTHS);
        testSubscription.setCreationDate(LocalDateTime.now());
        testSubscription.setLastModifiedDate(LocalDateTime.now());
        testSubscription = subscriptionRepository.save(testSubscription);
    }

    @Test
    void testSaveBalance() {
        Balance balance = createTestBalance();

        Balance savedBalance = balanceRepository.save(balance);

        assertThat(savedBalance).isNotNull();
        assertThat(savedBalance.getBalanceId()).isNotNull();
        assertThat(savedBalance.getSubscription().getSubscriptionId())
                .isEqualTo(testSubscription.getSubscriptionId());
        assertThat(savedBalance.getBalanceType()).isEqualTo(Balance.BalanceType.ALLOWANCE);
        assertThat(savedBalance.getUnitType()).isEqualTo(Balance.UnitType.BYTES);
    }

    @Test
    void testFindById() {
        Balance balance = createTestBalance();
        balance = balanceRepository.save(balance);

        Optional<Balance> foundBalance = balanceRepository.findById(balance.getBalanceId());

        assertThat(foundBalance).isPresent();
        assertThat(foundBalance.get().getBalanceId()).isEqualTo(balance.getBalanceId());
        assertThat(foundBalance.get().getBalanceAmount()).isEqualTo(10737418240L);
    }

    @Test
    void testFindById_NotFound() {
        Optional<Balance> foundBalance = balanceRepository.findById("NON-EXISTENT-ID");

        assertThat(foundBalance).isEmpty();
    }

    @Test
    void testFindBySubscription() {
        // Create multiple balances for the subscription
        Balance balance1 = createTestBalance();
        balance1.setBalanceType(Balance.BalanceType.ALLOWANCE);
        balance1.setUnitType(Balance.UnitType.BYTES);
        balanceRepository.save(balance1);

        Balance balance2 = createTestBalance();
        balance2.setBalanceType(Balance.BalanceType.COUNTER);
        balance2.setUnitType(Balance.UnitType.SECONDS);
        balance2.setBalanceAmount(3600L);
        balance2.setBalanceAvailable(3600L);
        balanceRepository.save(balance2);

        List<Balance> balances = balanceRepository.findBySubscription(testSubscription);

        assertThat(balances).hasSize(2);
        assertThat(balances)
                .extracting(Balance::getBalanceType)
                .containsExactlyInAnyOrder(Balance.BalanceType.ALLOWANCE, Balance.BalanceType.COUNTER);
    }

    @Test
    void testFindBySubscriptionId() {
        Balance balance = createTestBalance();
        balanceRepository.save(balance);

        List<Balance> balances = balanceRepository.findBySubscriptionId(testSubscription.getSubscriptionId());

        assertThat(balances).hasSize(1);
        assertThat(balances.get(0).getSubscription().getSubscriptionId())
                .isEqualTo(testSubscription.getSubscriptionId());
    }

    @Test
    @Transactional
    void testFindActiveBalances() {
        // Create active balance (future expiration, past effective date)
        Balance activeBalance = createTestBalance();
        activeBalance.setEffectiveDate(LocalDateTime.now().minusDays(1));  // Set to past
        activeBalance.setExpirationDate(LocalDateTime.now().plusDays(30));
        balanceRepository.save(activeBalance);

        // Create expired balance (past expiration)
        Balance expiredBalance = createTestBalance();
        expiredBalance.setBalanceId(UUID.randomUUID().toString());
        expiredBalance.setEffectiveDate(LocalDateTime.now().minusDays(2));  // Set to past
        expiredBalance.setExpirationDate(LocalDateTime.now().minusDays(1));
        balanceRepository.save(expiredBalance);

        // Create balance without expiration
        Balance noExpiryBalance = createTestBalance();
        noExpiryBalance.setBalanceId(UUID.randomUUID().toString());
        noExpiryBalance.setEffectiveDate(LocalDateTime.now().minusDays(1));  // Set to past
        noExpiryBalance.setExpirationDate(null);
        balanceRepository.save(noExpiryBalance);

        // Flush to ensure balances are persisted
        entityManager.flush();
        entityManager.clear();

        List<Balance> activeBalances = balanceRepository.findActiveBalances(
                testSubscription.getSubscriptionId(), 
                LocalDateTime.now()
        );

        // Should return active balance and no-expiry balance (2 total)
        assertThat(activeBalances).hasSize(2);
        assertThat(activeBalances)
                .extracting(Balance::getBalanceId)
                .doesNotContain(expiredBalance.getBalanceId());
    }

    @Test
    void testFindGroupBalances() {
        // Create group balance
        Balance groupBalance = createTestBalance();
        groupBalance.setIsGroupBalance(true);
        balanceRepository.save(groupBalance);

        // Create individual balance
        Balance individualBalance = createTestBalance();
        individualBalance.setBalanceId(UUID.randomUUID().toString());
        individualBalance.setIsGroupBalance(false);
        balanceRepository.save(individualBalance);

        List<Balance> groupBalances = balanceRepository.findGroupBalances(testSubscription.getSubscriptionId());

        assertThat(groupBalances).hasSize(1);
        assertThat(groupBalances.get(0).getIsGroupBalance()).isTrue();
    }

    @Test
    void testDeleteBalance() {
        Balance balance = createTestBalance();
        balance = balanceRepository.save(balance);
        String balanceId = balance.getBalanceId();

        balanceRepository.delete(balance);

        Optional<Balance> deletedBalance = balanceRepository.findById(balanceId);
        assertThat(deletedBalance).isEmpty();
    }

    @Test
    void testUpdateBalance() {
        Balance balance = createTestBalance();
        balance = balanceRepository.save(balance);

        // Update balance available
        balance.setBalanceAvailable(5368709120L); // 5GB remaining
        balance.setLastModifiedDate(LocalDateTime.now());
        Balance updatedBalance = balanceRepository.save(balance);

        assertThat(updatedBalance.getBalanceAvailable()).isEqualTo(5368709120L);
        assertThat(updatedBalance.getBalanceAmount()).isEqualTo(10737418240L); // Original amount unchanged
    }

    @Test
    void testFindRolloverEligibleBalances() {
        // Create rollover-eligible balance
        Balance rolloverBalance = createTestBalance();
        rolloverBalance.setIsRolloverAllowed(true);
        rolloverBalance.setMaxRolloverAmount(1073741824L); // 1GB max
        rolloverBalance.setBalanceAvailable(2147483648L); // 2GB available
        balanceRepository.save(rolloverBalance);

        // Create non-rollover balance
        Balance noRolloverBalance = createTestBalance();
        noRolloverBalance.setBalanceId(UUID.randomUUID().toString());
        noRolloverBalance.setIsRolloverAllowed(false);
        balanceRepository.save(noRolloverBalance);

        List<Balance> rolloverBalances = balanceRepository.findRolloverEligibleBalances(
                testSubscription.getSubscriptionId()
        );

        assertThat(rolloverBalances).hasSize(1);
        assertThat(rolloverBalances.get(0).getIsRolloverAllowed()).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testCascadeDelete_WhenSubscriptionDeleted() {
        // Run setup in a transaction
        Balance savedBalance = transactionTemplate.execute(status -> {
            Balance balance = createTestBalance();
            return balanceRepository.save(balance);
        });
        String balanceId = savedBalance.getBalanceId();

        // Delete subscription in another transaction (cascade delete happens at DB level)
        transactionTemplate.execute(status -> {
            subscriptionRepository.delete(testSubscription);
            return null;
        });

        // Verify balance was cascade deleted
        Optional<Balance> deletedBalance = balanceRepository.findById(balanceId);
        assertThat(deletedBalance).isEmpty();
    }

    /**
     * Helper method to create a test balance
     */
    private Balance createTestBalance() {
        Balance balance = new Balance();
        balance.setBalanceId(UUID.randomUUID().toString());
        balance.setSubscriptionId(testSubscription.getSubscriptionId()); // Set subscriptionId for validation
        balance.setSubscription(testSubscription);
        balance.setBalanceType(Balance.BalanceType.ALLOWANCE);
        balance.setUnitType(Balance.UnitType.BYTES);
        balance.setBalanceAmount(10737418240L); // 10GB
        balance.setBalanceAvailable(10737418240L);
        balance.setEffectiveDate(LocalDateTime.now());
        balance.setExpirationDate(LocalDateTime.now().plusDays(30));
        balance.setIsRolloverAllowed(false);
        balance.setIsRecurring(false);
        balance.setIsGroupBalance(false);
        balance.setCreationDate(LocalDateTime.now());
        balance.setLastModifiedDate(LocalDateTime.now());
        return balance;
    }
}
