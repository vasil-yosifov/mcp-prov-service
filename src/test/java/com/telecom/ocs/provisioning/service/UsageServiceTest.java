package com.telecom.ocs.provisioning.service;

import com.telecom.ocs.provisioning.exceptions.DuplicateResourceException;
import com.telecom.ocs.provisioning.exceptions.ResourceNotFoundException;
import com.telecom.ocs.provisioning.models.Balance;
import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.models.Usage;
import com.telecom.ocs.provisioning.repositories.BalanceRepository;
import com.telecom.ocs.provisioning.repositories.SubscriberRepository;
import com.telecom.ocs.provisioning.repositories.UsageRepository;
import com.telecom.ocs.provisioning.services.UsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * T177: Unit tests for UsageService with Mockito.
 * Tests business logic including balance update logic (FR-096, FR-097, FR-098).
 * 
 * Balance update logic:
 * - FR-096: ALLOWANCE balance deduction (balanceAvailable -= volumeUsage)
 * - FR-097: ALLOWANCE floor at 0 (no negative balance)
 * - FR-098: COUNTER balance addition (balanceAvailable += volumeUsage)
 */
@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private UsageRepository usageRepository;

    @Mock
    private SubscriberRepository subscriberRepository;

    @Mock
    private BalanceRepository balanceRepository;

    @InjectMocks
    private UsageService usageService;

    private Subscriber testSubscriber;
    private Balance allowanceBalance;
    private Balance counterBalance;
    private Usage testUsage;

    @BeforeEach
    void setUp() {
        testSubscriber = new Subscriber();
        testSubscriber.setSubscriberId(UUID.randomUUID().toString());
        testSubscriber.setMsisdn("436641234567");
        testSubscriber.setFirstName("Test");
        testSubscriber.setLastName("Subscriber");
        testSubscriber.setCreationDate(LocalDateTime.now());
        testSubscriber.setLastModifiedDate(LocalDateTime.now());

        allowanceBalance = new Balance();
        allowanceBalance.setBalanceId(UUID.randomUUID().toString());
        allowanceBalance.setBalanceType(Balance.BalanceType.ALLOWANCE);
        allowanceBalance.setUnitType(Balance.UnitType.SECONDS);
        allowanceBalance.setBalanceAmount(1000L);
        allowanceBalance.setBalanceAvailable(1000L);
        allowanceBalance.setIsRolloverAllowed(false);
        allowanceBalance.setIsRecurring(true);
        allowanceBalance.setIsGroupBalance(false);
        allowanceBalance.setCreationDate(LocalDateTime.now());
        allowanceBalance.setLastModifiedDate(LocalDateTime.now());

        counterBalance = new Balance();
        counterBalance.setBalanceId(UUID.randomUUID().toString());
        counterBalance.setBalanceType(Balance.BalanceType.COUNTER);
        counterBalance.setUnitType(Balance.UnitType.EVENTS);
        counterBalance.setBalanceAmount(500L);
        counterBalance.setBalanceAvailable(500L);
        counterBalance.setIsRolloverAllowed(false);
        counterBalance.setIsRecurring(false);
        counterBalance.setIsGroupBalance(false);
        counterBalance.setCreationDate(LocalDateTime.now());
        counterBalance.setLastModifiedDate(LocalDateTime.now());

        testUsage = new Usage();
        testUsage.setUsageId(UUID.randomUUID().toString());
        testUsage.setUsageTimestamp(LocalDateTime.now());
        testUsage.setChargedPartyId(testSubscriber.getSubscriberId());
        testUsage.setChargedMsisdn(testSubscriber.getMsisdn());
        testUsage.setUsageType(Usage.UsageType.VOICE);
        testUsage.setRecordType(Usage.RecordType.EVENT);
        testUsage.setVolumeUsage(60L);
        testUsage.setImpactedBalanceId(allowanceBalance.getBalanceId());
    }

    /**
     * Test creating usage record with ALLOWANCE balance deduction (FR-096)
     * Expected: balanceAvailable decreased by volumeUsage
     */
    @Test
    void testCreateUsage_AllowanceBalanceDeduction() {
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.of(testSubscriber));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(allowanceBalance));
        when(usageRepository.existsById(anyString())).thenReturn(false);
        when(usageRepository.save(any(Usage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        testUsage.setVolumeUsage(300L); // Use 300 seconds from 1000
        Usage created = usageService.createUsage(testUsage);

        assertThat(created).isNotNull();
        assertThat(created.getBalanceValueBefore()).isEqualTo(1000L);
        assertThat(created.getBalanceValueAfter()).isEqualTo(700L); // 1000 - 300
        
        verify(balanceRepository, times(1)).save(argThat(balance -> 
                balance.getBalanceAvailable().equals(700L)));
        verify(usageRepository, times(1)).save(any(Usage.class));
    }

    /**
     * Test ALLOWANCE balance floor at 0 when volumeUsage exceeds available (FR-097)
     * Expected: balanceAvailable set to 0 (not negative)
     */
    @Test
    void testCreateUsage_AllowanceFloorAtZero() {
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.of(testSubscriber));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(allowanceBalance));
        when(usageRepository.existsById(anyString())).thenReturn(false);
        when(usageRepository.save(any(Usage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        testUsage.setVolumeUsage(1500L); // Exceed 1000 available
        Usage created = usageService.createUsage(testUsage);

        assertThat(created).isNotNull();
        assertThat(created.getBalanceValueBefore()).isEqualTo(1000L);
        assertThat(created.getBalanceValueAfter()).isEqualTo(0L); // Floor at 0, not -500
        
        verify(balanceRepository, times(1)).save(argThat(balance -> 
                balance.getBalanceAvailable().equals(0L)));
    }

    /**
     * Test COUNTER balance addition (FR-098)
     * Expected: balanceAvailable increased by volumeUsage
     */
    @Test
    void testCreateUsage_CounterBalanceAddition() {
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.of(testSubscriber));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(counterBalance));
        when(usageRepository.existsById(anyString())).thenReturn(false);
        when(usageRepository.save(any(Usage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        testUsage.setImpactedBalanceId(counterBalance.getBalanceId());
        testUsage.setVolumeUsage(100L); // Add 100 to 500
        Usage created = usageService.createUsage(testUsage);

        assertThat(created).isNotNull();
        assertThat(created.getBalanceValueBefore()).isEqualTo(500L);
        assertThat(created.getBalanceValueAfter()).isEqualTo(600L); // 500 + 100
        
        verify(balanceRepository, times(1)).save(argThat(balance -> 
                balance.getBalanceAvailable().equals(600L)));
    }

    /**
     * Test creating usage record with valid subscriber and balance
     */
    @Test
    void testCreateUsage_Success() {
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.of(testSubscriber));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(allowanceBalance));
        when(usageRepository.existsById(anyString())).thenReturn(false);
        when(usageRepository.save(any(Usage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usage created = usageService.createUsage(testUsage);

        assertThat(created).isNotNull();
        assertThat(created.getUsageId()).isEqualTo(testUsage.getUsageId());
        assertThat(created.getChargedPartyId()).isEqualTo(testSubscriber.getSubscriberId());
        
        verify(subscriberRepository, times(1)).findById(testSubscriber.getSubscriberId());
        verify(balanceRepository, times(1)).findById(allowanceBalance.getBalanceId());
        verify(usageRepository, times(1)).save(any(Usage.class));
    }

    /**
     * Test creating usage with non-existent subscriber (FR-091)
     * Expected: ResourceNotFoundException
     */
    @Test
    void testCreateUsage_SubscriberNotFound() {
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usageService.createUsage(testUsage))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Subscriber not found");

        verify(subscriberRepository, times(1)).findById(testSubscriber.getSubscriberId());
        verify(usageRepository, never()).save(any(Usage.class));
    }

    /**
     * Test creating usage with non-existent balance
     * Expected: ResourceNotFoundException
     */
    @Test
    void testCreateUsage_BalanceNotFound() {
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.of(testSubscriber));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usageService.createUsage(testUsage))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Balance not found");

        verify(balanceRepository, times(1)).findById(allowanceBalance.getBalanceId());
        verify(usageRepository, never()).save(any(Usage.class));
    }

    /**
     * Test creating usage with duplicate usageId (FR-092)
     * Expected: DuplicateResourceException with 409 Conflict
     */
    @Test
    void testCreateUsage_DuplicateUsageId() {
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.of(testSubscriber));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(allowanceBalance));
        when(usageRepository.existsById(anyString())).thenReturn(true); // Duplicate

        assertThatThrownBy(() -> usageService.createUsage(testUsage))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Usage record already exists");

        verify(usageRepository, times(1)).existsById(testUsage.getUsageId());
        verify(usageRepository, never()).save(any(Usage.class));
    }

    /**
     * Test retrieving usage by usageId
     */
    @Test
    void testGetUsageById_Success() {
        when(usageRepository.findById(anyString())).thenReturn(Optional.of(testUsage));

        Usage found = usageService.getUsageById(testUsage.getUsageId());

        assertThat(found).isNotNull();
        assertThat(found.getUsageId()).isEqualTo(testUsage.getUsageId());
        verify(usageRepository, times(1)).findById(testUsage.getUsageId());
    }

    /**
     * Test retrieving non-existent usage
     */
    @Test
    void testGetUsageById_NotFound() {
        when(usageRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usageService.getUsageById("INVALID-USAGE-ID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usage record not found");

        verify(usageRepository, times(1)).findById("INVALID-USAGE-ID");
    }

    /**
     * Test balance value capture before and after usage
     */
    @Test
    void testCreateUsage_BalanceValueCapture() {
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.of(testSubscriber));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(allowanceBalance));
        when(usageRepository.existsById(anyString())).thenReturn(false);
        when(usageRepository.save(any(Usage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        testUsage.setVolumeUsage(200L);
        Usage created = usageService.createUsage(testUsage);

        // Verify balanceValueBefore and balanceValueAfter are correctly set
        assertThat(created.getBalanceValueBefore()).isEqualTo(1000L);
        assertThat(created.getBalanceValueAfter()).isEqualTo(800L);
    }

    /**
     * Test voice usage with session duration
     */
    @Test
    void testCreateUsage_VoiceWithDuration() {
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.of(testSubscriber));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(allowanceBalance));
        when(usageRepository.existsById(anyString())).thenReturn(false);
        when(usageRepository.save(any(Usage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        testUsage.setRecordType(Usage.RecordType.STOP);
        testUsage.setRecordOpeningTime(LocalDateTime.now().minusMinutes(5));
        testUsage.setRecordClosingTime(LocalDateTime.now());
        testUsage.setDurationSeconds(300);
        testUsage.setVolumeUsage(300L);
        testUsage.setAParty("43664111111");
        testUsage.setBParty("43664222222");

        Usage created = usageService.createUsage(testUsage);

        assertThat(created.getDurationSeconds()).isEqualTo(300);
        assertThat(created.getRecordType()).isEqualTo(Usage.RecordType.STOP);
        assertThat(created.getAParty()).isEqualTo("43664111111");
        assertThat(created.getBParty()).isEqualTo("43664222222");
    }

    /**
     * Test data usage with APN
     */
    @Test
    void testCreateUsage_DataWithApn() {
        Balance dataBalance = new Balance();
        dataBalance.setBalanceId(UUID.randomUUID().toString());
        dataBalance.setBalanceType(Balance.BalanceType.ALLOWANCE);
        dataBalance.setUnitType(Balance.UnitType.BYTES);
        dataBalance.setBalanceAmount(104857600L); // 100MB
        dataBalance.setBalanceAvailable(104857600L);
        dataBalance.setIsRolloverAllowed(false);
        dataBalance.setIsRecurring(true);
        dataBalance.setIsGroupBalance(false);

        when(subscriberRepository.findById(anyString())).thenReturn(Optional.of(testSubscriber));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(dataBalance));
        when(usageRepository.existsById(anyString())).thenReturn(false);
        when(usageRepository.save(any(Usage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        testUsage.setUsageType(Usage.UsageType.DATA);
        testUsage.setImpactedBalanceId(dataBalance.getBalanceId());
        testUsage.setVolumeUsage(52428800L); // 50MB
        testUsage.setBParty("internet.apn");

        Usage created = usageService.createUsage(testUsage);

        assertThat(created.getUsageType()).isEqualTo(Usage.UsageType.DATA);
        assertThat(created.getVolumeUsage()).isEqualTo(52428800L);
        assertThat(created.getBParty()).isEqualTo("internet.apn");
        assertThat(created.getBalanceValueAfter()).isEqualTo(52428800L); // 100MB - 50MB
    }

    /**
     * Test SMS event usage
     */
    @Test
    void testCreateUsage_SmsEvent() {
        Balance smsBalance = new Balance();
        smsBalance.setBalanceId(UUID.randomUUID().toString());
        smsBalance.setBalanceType(Balance.BalanceType.ALLOWANCE);
        smsBalance.setUnitType(Balance.UnitType.EVENTS);
        smsBalance.setBalanceAmount(100L);
        smsBalance.setBalanceAvailable(100L);
        smsBalance.setIsRolloverAllowed(false);
        smsBalance.setIsRecurring(true);
        smsBalance.setIsGroupBalance(false);

        when(subscriberRepository.findById(anyString())).thenReturn(Optional.of(testSubscriber));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(smsBalance));
        when(usageRepository.existsById(anyString())).thenReturn(false);
        when(usageRepository.save(any(Usage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        testUsage.setUsageType(Usage.UsageType.SMS);
        testUsage.setRecordType(Usage.RecordType.EVENT);
        testUsage.setImpactedBalanceId(smsBalance.getBalanceId());
        testUsage.setVolumeUsage(1L);
        testUsage.setBParty("43664999999");

        Usage created = usageService.createUsage(testUsage);

        assertThat(created.getUsageType()).isEqualTo(Usage.UsageType.SMS);
        assertThat(created.getRecordType()).isEqualTo(Usage.RecordType.EVENT);
        assertThat(created.getBalanceValueAfter()).isEqualTo(99L); // 100 - 1
    }
}
