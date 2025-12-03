package com.telecom.ocs.provisioning.service;

import com.telecom.ocs.provisioning.exceptions.ResourceNotFoundException;
import com.telecom.ocs.provisioning.models.Balance;
import com.telecom.ocs.provisioning.models.Subscription;
import com.telecom.ocs.provisioning.repositories.BalanceRepository;
import com.telecom.ocs.provisioning.repositories.SubscriptionRepository;
import com.telecom.ocs.provisioning.services.BalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * T078: Unit tests for BalanceService.
 * Tests business logic using Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private BalanceService balanceService;

    private Balance testBalance;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        testSubscription = new Subscription();
        testSubscription.setSubscriptionId(UUID.randomUUID().toString());
        testSubscription.setOfferId("DATA-PLAN-001");
        testSubscription.setOfferName("10GB Monthly Plan");
        testSubscription.setState(Subscription.SubscriptionState.ACTIVE);

        testBalance = new Balance();
        testBalance.setBalanceId(UUID.randomUUID().toString());
        testBalance.setSubscription(testSubscription);
        testBalance.setBalanceType(Balance.BalanceType.ALLOWANCE);
        testBalance.setUnitType(Balance.UnitType.BYTES);
        testBalance.setBalanceAmount(10737418240L); // 10GB
        testBalance.setBalanceAvailable(10737418240L);
        testBalance.setEffectiveDate(LocalDateTime.now());
        testBalance.setExpirationDate(LocalDateTime.now().plusDays(30));
        testBalance.setIsRolloverAllowed(false);
        testBalance.setIsRecurring(false);
        testBalance.setIsGroupBalance(false);
        testBalance.setCreationDate(LocalDateTime.now());
        testBalance.setLastModifiedDate(LocalDateTime.now());
    }

    @Test
    void testCreateBalance_Success() {
        when(subscriptionRepository.findById(anyString())).thenReturn(Optional.of(testSubscription));
        when(balanceRepository.save(any(Balance.class))).thenReturn(testBalance);

        Balance createdBalance = balanceService.createBalance(testSubscription.getSubscriptionId(), testBalance);

        assertThat(createdBalance).isNotNull();
        assertThat(createdBalance.getBalanceId()).isEqualTo(testBalance.getBalanceId());
        verify(subscriptionRepository, times(1)).findById(testSubscription.getSubscriptionId());
        verify(balanceRepository, times(1)).save(any(Balance.class));
    }

    @Test
    void testCreateBalance_SubscriptionNotFound() {
        when(subscriptionRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceService.createBalance("INVALID-SUB-ID", testBalance))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Subscription not found");

        verify(subscriptionRepository, times(1)).findById("INVALID-SUB-ID");
        verify(balanceRepository, never()).save(any(Balance.class));
    }

    @Test
    void testGetBalanceById_Success() {
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));

        Balance foundBalance = balanceService.getBalanceById(testBalance.getBalanceId());

        assertThat(foundBalance).isNotNull();
        assertThat(foundBalance.getBalanceId()).isEqualTo(testBalance.getBalanceId());
        verify(balanceRepository, times(1)).findById(testBalance.getBalanceId());
    }

    @Test
    void testGetBalanceById_NotFound() {
        when(balanceRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceService.getBalanceById("INVALID-BALANCE-ID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Balance not found");

        verify(balanceRepository, times(1)).findById("INVALID-BALANCE-ID");
    }

    @Test
    void testGetBalancesBySubscriptionId() {
        Balance balance2 = new Balance();
        balance2.setBalanceId(UUID.randomUUID().toString());
        balance2.setSubscription(testSubscription);
        balance2.setBalanceType(Balance.BalanceType.COUNTER);
        balance2.setUnitType(Balance.UnitType.SECONDS);

        List<Balance> balances = Arrays.asList(testBalance, balance2);
        when(balanceRepository.findBySubscriptionId(anyString())).thenReturn(balances);

        List<Balance> foundBalances = balanceService.getBalancesBySubscriptionId(testSubscription.getSubscriptionId());

        assertThat(foundBalances).hasSize(2);
        assertThat(foundBalances).extracting(Balance::getBalanceType)
                .containsExactlyInAnyOrder(Balance.BalanceType.ALLOWANCE, Balance.BalanceType.COUNTER);
        verify(balanceRepository, times(1)).findBySubscriptionId(testSubscription.getSubscriptionId());
    }

    @Test
    void testUpdateBalance_Success() {
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(any(Balance.class))).thenReturn(testBalance);

        testBalance.setBalanceAvailable(5368709120L); // 5GB remaining
        Balance updatedBalance = balanceService.updateBalance(testBalance.getBalanceId(), testBalance);

        assertThat(updatedBalance.getBalanceAvailable()).isEqualTo(5368709120L);
        verify(balanceRepository, times(1)).findById(testBalance.getBalanceId());
        verify(balanceRepository, times(1)).save(any(Balance.class));
    }

    @Test
    void testUpdateBalance_NotFound() {
        when(balanceRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceService.updateBalance("INVALID-BALANCE-ID", testBalance))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Balance not found");

        verify(balanceRepository, times(1)).findById("INVALID-BALANCE-ID");
        verify(balanceRepository, never()).save(any(Balance.class));
    }

    @Test
    void testDeleteBalance_Success() {
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));
        doNothing().when(balanceRepository).delete(any(Balance.class));

        balanceService.deleteBalance(testBalance.getBalanceId());

        verify(balanceRepository, times(1)).findById(testBalance.getBalanceId());
        verify(balanceRepository, times(1)).delete(testBalance);
    }

    @Test
    void testDeleteBalance_NotFound() {
        when(balanceRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceService.deleteBalance("INVALID-BALANCE-ID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Balance not found");

        verify(balanceRepository, times(1)).findById("INVALID-BALANCE-ID");
        verify(balanceRepository, never()).delete(any(Balance.class));
    }

    @Test
    void testGetActiveBalances() {
        Balance activeBalance = new Balance();
        activeBalance.setBalanceId(UUID.randomUUID().toString());
        activeBalance.setExpirationDate(LocalDateTime.now().plusDays(30));

        List<Balance> activeBalances = Arrays.asList(activeBalance);
        when(balanceRepository.findActiveBalances(anyString(), any(LocalDateTime.class)))
                .thenReturn(activeBalances);

        List<Balance> result = balanceService.getActiveBalances(testSubscription.getSubscriptionId());

        assertThat(result).hasSize(1);
        verify(balanceRepository, times(1))
                .findActiveBalances(eq(testSubscription.getSubscriptionId()), any(LocalDateTime.class));
    }

    @Test
    void testProcessRollover_CappedAtMaxRolloverAmount() {
        // Setup balance with rollover allowed
        testBalance.setIsRolloverAllowed(true);
        testBalance.setMaxRolloverAmount(1073741824L); // 1GB max rollover
        testBalance.setBalanceAvailable(2147483648L); // 2GB available
        testBalance.setRolloverAmount(0L);

        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Balance rolledOverBalance = balanceService.processRollover(testBalance.getBalanceId());

        // rolloverAmount should be capped at maxRolloverAmount (1GB)
        assertThat(rolledOverBalance.getRolloverAmount()).isLessThanOrEqualTo(1073741824L);
        verify(balanceRepository, times(1)).findById(testBalance.getBalanceId());
        verify(balanceRepository, times(1)).save(any(Balance.class));
    }

    @Test
    void testProcessRollover_NotAllowed() {
        testBalance.setIsRolloverAllowed(false);
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));

        assertThatThrownBy(() -> balanceService.processRollover(testBalance.getBalanceId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Rollover is not allowed");

        verify(balanceRepository, times(1)).findById(testBalance.getBalanceId());
        verify(balanceRepository, never()).save(any(Balance.class));
    }

    @Test
    void testIsBalanceExpired_Expired() {
        testBalance.setExpirationDate(LocalDateTime.now().minusDays(1));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));

        boolean isExpired = balanceService.isBalanceExpired(testBalance.getBalanceId());

        assertThat(isExpired).isTrue();
    }

    @Test
    void testIsBalanceExpired_Active() {
        testBalance.setExpirationDate(LocalDateTime.now().plusDays(30));
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));

        boolean isExpired = balanceService.isBalanceExpired(testBalance.getBalanceId());

        assertThat(isExpired).isFalse();
    }

    @Test
    void testIsBalanceExpired_NoExpirationDate() {
        testBalance.setExpirationDate(null);
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));

        boolean isExpired = balanceService.isBalanceExpired(testBalance.getBalanceId());

        assertThat(isExpired).isFalse(); // No expiration date means never expires
    }

    @Test
    void testGetGroupBalances() {
        testBalance.setIsGroupBalance(true);
        List<Balance> groupBalances = Arrays.asList(testBalance);

        when(balanceRepository.findGroupBalances(anyString())).thenReturn(groupBalances);

        List<Balance> result = balanceService.getGroupBalances(testSubscription.getSubscriptionId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsGroupBalance()).isTrue();
        verify(balanceRepository, times(1)).findGroupBalances(testSubscription.getSubscriptionId());
    }

    @Test
    void testDeductBalance_Success() {
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long deductAmount = 1073741824L; // Deduct 1GB
        Balance deductedBalance = balanceService.deductBalance(testBalance.getBalanceId(), deductAmount);

        assertThat(deductedBalance.getBalanceAvailable()).isEqualTo(10737418240L - 1073741824L);
        verify(balanceRepository, times(1)).save(any(Balance.class));
    }

    @Test
    void testDeductBalance_InsufficientBalance() {
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));

        long excessiveDeductAmount = 21474836480L; // 20GB - more than available

        assertThatThrownBy(() -> balanceService.deductBalance(testBalance.getBalanceId(), excessiveDeductAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient balance");

        verify(balanceRepository, times(1)).findById(testBalance.getBalanceId());
        verify(balanceRepository, never()).save(any(Balance.class));
    }

    @Test
    void testRefundBalance_Success() {
        testBalance.setBalanceAvailable(5368709120L); // 5GB available
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long refundAmount = 1073741824L; // Refund 1GB
        Balance refundedBalance = balanceService.refundBalance(testBalance.getBalanceId(), refundAmount);

        assertThat(refundedBalance.getBalanceAvailable()).isEqualTo(5368709120L + 1073741824L);
        verify(balanceRepository, times(1)).save(any(Balance.class));
    }

    @Test
    void testRefundBalance_CannotExceedTotalAmount() {
        testBalance.setBalanceAvailable(9663676416L); // 9GB available out of 10GB total
        when(balanceRepository.findById(anyString())).thenReturn(Optional.of(testBalance));

        long excessiveRefundAmount = 2147483648L; // 2GB - would exceed total balance

        assertThatThrownBy(() -> balanceService.refundBalance(testBalance.getBalanceId(), excessiveRefundAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refund would exceed total balance");

        verify(balanceRepository, times(1)).findById(testBalance.getBalanceId());
        verify(balanceRepository, never()).save(any(Balance.class));
    }
}
