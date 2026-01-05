package com.telecom.ocs.provisioning.repository;

import com.telecom.ocs.provisioning.models.Balance;
import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.models.Subscription;
import com.telecom.ocs.provisioning.models.Usage;
import com.telecom.ocs.provisioning.repositories.BalanceRepository;
import com.telecom.ocs.provisioning.repositories.SubscriberRepository;
import com.telecom.ocs.provisioning.repositories.SubscriptionRepository;
import com.telecom.ocs.provisioning.repositories.UsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
 * T176: Repository tests for UsageRepository.
 * Tests data access layer operations for Usage entity including pagination queries.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UsageRepositoryTest {

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
    private UsageRepository usageRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Subscriber testSubscriber;
    private Subscription testSubscription;
    private Balance testBalance;

    @BeforeEach
    void setUp() {
        // Create test subscriber
        testSubscriber = new Subscriber();
        testSubscriber.setSubscriberId(UUID.randomUUID().toString());
        testSubscriber.setMsisdn("436641234567");
        testSubscriber.setFirstName("Usage");
        testSubscriber.setLastName("Test");
        testSubscriber.setCreationDate(LocalDateTime.now());
        testSubscriber.setLastModifiedDate(LocalDateTime.now());
        testSubscriber = subscriberRepository.save(testSubscriber);

        // Create test subscription
        testSubscription = new Subscription();
        testSubscription.setSubscriptionId(UUID.randomUUID().toString());
        testSubscription.setSubscriberId(testSubscriber.getSubscriberId());
        testSubscription.setSubscriber(testSubscriber);
        testSubscription.setOfferId("VOICE-PLAN-001");
        testSubscription.setOfferName("Voice Plan");
        testSubscription.setSubscriptionType("VOICE");
        testSubscription.setState(Subscription.SubscriptionState.ACTIVE);
        testSubscription.setRecurring(true);
        testSubscription.setMaxRecurringCycles(12);
        testSubscription.setCycleLengthUnits(1);
        testSubscription.setCycleLengthType(Subscription.CycleLengthType.MONTHS);
        testSubscription.setCreationDate(LocalDateTime.now());
        testSubscription.setLastModifiedDate(LocalDateTime.now());
        testSubscription = subscriptionRepository.save(testSubscription);

        // Create test balance
        testBalance = new Balance();
        testBalance.setBalanceId(UUID.randomUUID().toString());
        testBalance.setSubscriptionId(testSubscription.getSubscriptionId());
        testBalance.setSubscription(testSubscription);
        testBalance.setBalanceType(Balance.BalanceType.ALLOWANCE);
        testBalance.setUnitType(Balance.UnitType.SECONDS);
        testBalance.setBalanceAmount(1000L);
        testBalance.setBalanceAvailable(1000L);
        testBalance.setIsRolloverAllowed(false);
        testBalance.setIsRecurring(true);
        testBalance.setIsGroupBalance(false);
        testBalance = balanceRepository.save(testBalance);
    }

    /**
     * Test saving a new usage record
     */
    @Test
    void testSaveUsageRecord() {
        Usage usage = createUsageRecord("VOICE", "EVENT", 60L);
        
        Usage saved = usageRepository.save(usage);
        
        assertThat(saved).isNotNull();
        assertThat(saved.getUsageId()).isEqualTo(usage.getUsageId());
        assertThat(saved.getChargedPartyId()).isEqualTo(testSubscriber.getSubscriberId());
        assertThat(saved.getUsageType()).isEqualTo(Usage.UsageType.VOICE);
        assertThat(saved.getRecordType()).isEqualTo(Usage.RecordType.EVENT);
        assertThat(saved.getVolumeUsage()).isEqualTo(60L);
        assertThat(saved.getImpactedBalanceId()).isEqualTo(testBalance.getBalanceId());
    }

    /**
     * Test finding usage record by usageId
     */
    @Test
    void testFindByUsageId() {
        Usage usage = createUsageRecord("DATA", "STOP", 1048576L);
        usageRepository.save(usage);
        entityManager.flush();
        
        Optional<Usage> found = usageRepository.findById(usage.getUsageId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getUsageId()).isEqualTo(usage.getUsageId());
        assertThat(found.get().getUsageType()).isEqualTo(Usage.UsageType.DATA);
        assertThat(found.get().getVolumeUsage()).isEqualTo(1048576L);
    }

    /**
     * Test finding usage records by chargedPartyId (subscriber)
     */
    @Test
    void testFindByChargedPartyId() {
        // Create multiple usage records
        Usage usage1 = createUsageRecord("VOICE", "EVENT", 60L);
        Usage usage2 = createUsageRecord("DATA", "STOP", 524288L);
        Usage usage3 = createUsageRecord("SMS", "EVENT", 1L);
        
        usageRepository.save(usage1);
        usageRepository.save(usage2);
        usageRepository.save(usage3);
        entityManager.flush();
        
        List<Usage> usages = usageRepository.findByChargedPartyId(testSubscriber.getSubscriberId());
        
        assertThat(usages).hasSize(3);
        assertThat(usages).extracting(Usage::getUsageType)
                .containsExactlyInAnyOrder(Usage.UsageType.VOICE, Usage.UsageType.DATA, Usage.UsageType.SMS);
    }

    /**
     * Test finding usage records by chargedPartyId with pagination
     */
    @Test
    void testFindByChargedPartyIdWithPagination() {
        // Create 5 usage records
        for (int i = 0; i < 5; i++) {
            Usage usage = createUsageRecord("VOICE", "EVENT", 30L + i);
            usageRepository.save(usage);
        }
        entityManager.flush();
        
        // Get first page (2 records)
        Page<Usage> page1 = usageRepository.findByChargedPartyId(
                testSubscriber.getSubscriberId(), PageRequest.of(0, 2));
        
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(5);
        assertThat(page1.getTotalPages()).isEqualTo(3);
        assertThat(page1.hasNext()).isTrue();
        
        // Get second page (2 records)
        Page<Usage> page2 = usageRepository.findByChargedPartyId(
                testSubscriber.getSubscriberId(), PageRequest.of(1, 2));
        
        assertThat(page2.getContent()).hasSize(2);
        assertThat(page2.getNumber()).isEqualTo(1);
        
        // Get last page (1 record)
        Page<Usage> page3 = usageRepository.findByChargedPartyId(
                testSubscriber.getSubscriberId(), PageRequest.of(2, 2));
        
        assertThat(page3.getContent()).hasSize(1);
        assertThat(page3.hasNext()).isFalse();
    }

    /**
     * Test finding usage records by impactedBalanceId
     */
    @Test
    void testFindByImpactedBalanceId() {
        // Create usage records impacting the same balance
        Usage usage1 = createUsageRecord("VOICE", "EVENT", 60L);
        Usage usage2 = createUsageRecord("VOICE", "STOP", 180L);
        
        usageRepository.save(usage1);
        usageRepository.save(usage2);
        entityManager.flush();
        
        List<Usage> usages = usageRepository.findByImpactedBalanceId(testBalance.getBalanceId());
        
        assertThat(usages).hasSize(2);
        assertThat(usages).allMatch(u -> u.getImpactedBalanceId().equals(testBalance.getBalanceId()));
    }

    /**
     * Test finding usage records by usageType
     */
    @Test
    void testFindByUsageType() {
        // Create mixed usage types
        Usage voiceUsage1 = createUsageRecord("VOICE", "EVENT", 60L);
        Usage voiceUsage2 = createUsageRecord("VOICE", "STOP", 120L);
        Usage dataUsage = createUsageRecord("DATA", "STOP", 1048576L);
        
        usageRepository.save(voiceUsage1);
        usageRepository.save(voiceUsage2);
        usageRepository.save(dataUsage);
        entityManager.flush();
        
        List<Usage> voiceUsages = usageRepository.findByUsageType(Usage.UsageType.VOICE);
        
        assertThat(voiceUsages).hasSize(2);
        assertThat(voiceUsages).allMatch(u -> u.getUsageType() == Usage.UsageType.VOICE);
    }

    /**
     * Test finding usage records by recordType
     */
    @Test
    void testFindByRecordType() {
        Usage eventUsage1 = createUsageRecord("VOICE", "EVENT", 60L);
        Usage eventUsage2 = createUsageRecord("SMS", "EVENT", 1L);
        Usage stopUsage = createUsageRecord("DATA", "STOP", 524288L);
        
        usageRepository.save(eventUsage1);
        usageRepository.save(eventUsage2);
        usageRepository.save(stopUsage);
        entityManager.flush();
        
        List<Usage> eventUsages = usageRepository.findByRecordType(Usage.RecordType.EVENT);
        
        assertThat(eventUsages).hasSize(2);
        assertThat(eventUsages).allMatch(u -> u.getRecordType() == Usage.RecordType.EVENT);
    }

    /**
     * Test finding usage records within date range
     */
    @Test
    void testFindByUsageTimestampBetween() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        
        Usage usage1 = createUsageRecord("VOICE", "EVENT", 60L);
        usage1.setUsageTimestamp(LocalDateTime.now().minusMinutes(30));
        
        Usage usage2 = createUsageRecord("DATA", "STOP", 1048576L);
        usage2.setUsageTimestamp(LocalDateTime.now().minusMinutes(15));
        
        Usage usage3 = createUsageRecord("SMS", "EVENT", 1L);
        usage3.setUsageTimestamp(LocalDateTime.now().minusHours(2)); // Outside range
        
        usageRepository.save(usage1);
        usageRepository.save(usage2);
        usageRepository.save(usage3);
        entityManager.flush();
        
        List<Usage> usages = usageRepository.findByUsageTimestampBetween(start, end);
        
        assertThat(usages).hasSize(2);
        assertThat(usages).extracting(Usage::getUsageId)
                .containsExactlyInAnyOrder(usage1.getUsageId(), usage2.getUsageId());
    }

    /**
     * Test deleting usage record
     */
    @Test
    void testDeleteUsageRecord() {
        Usage usage = createUsageRecord("VOICE", "EVENT", 60L);
        usageRepository.save(usage);
        entityManager.flush();
        
        String usageId = usage.getUsageId();
        assertThat(usageRepository.findById(usageId)).isPresent();
        
        usageRepository.deleteById(usageId);
        entityManager.flush();
        
        assertThat(usageRepository.findById(usageId)).isEmpty();
    }

    /**
     * Test counting usage records by chargedPartyId
     */
    @Test
    void testCountByChargedPartyId() {
        // Create 3 usage records for same subscriber
        for (int i = 0; i < 3; i++) {
            Usage usage = createUsageRecord("VOICE", "EVENT", 60L + i);
            usageRepository.save(usage);
        }
        entityManager.flush();
        
        long count = usageRepository.countByChargedPartyId(testSubscriber.getSubscriberId());
        
        assertThat(count).isEqualTo(3);
    }

    /**
     * Helper method to create usage record with common fields
     */
    private Usage createUsageRecord(String usageType, String recordType, Long volumeUsage) {
        Usage usage = new Usage();
        usage.setUsageId(UUID.randomUUID().toString());
        usage.setUsageTimestamp(LocalDateTime.now());
        usage.setChargedPartyId(testSubscriber.getSubscriberId());
        usage.setChargedMsisdn(testSubscriber.getMsisdn());
        usage.setUsageType(Usage.UsageType.valueOf(usageType));
        usage.setRecordType(Usage.RecordType.valueOf(recordType));
        usage.setVolumeUsage(volumeUsage);
        usage.setImpactedBalanceId(testBalance.getBalanceId());
        usage.setBalanceValueBefore(1000L);
        usage.setBalanceValueAfter(1000L - volumeUsage);
        return usage;
    }
}
