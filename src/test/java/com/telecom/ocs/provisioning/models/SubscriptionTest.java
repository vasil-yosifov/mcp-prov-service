package com.telecom.ocs.provisioning.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Subscription entity.
 * Tests the business logic methods within the entity itself.
 */
class SubscriptionTest {

    private Subscription subscription;

    @BeforeEach
    void setUp() {
        subscription = Subscription.builder()
                .subscriptionId("test-sub-id")
                .subscriberId("test-subscriber-id")
                .offerId("OFFER-001")
                .offerName("Premium Data Plan")
                .state(Subscription.SubscriptionState.PENDING)
                .recurring(true)
                .maxRecurringCycles(12)
                .recurringCyclesCompleted(0)
                .cycleLengthType(Subscription.CycleLengthType.MONTHS)
                .cycleLengthUnits(1)
                .build();
    }

    // =========================================================================
    // State Transition Tests
    // =========================================================================

    @Test
    @DisplayName("transitionTo should change state and set activation date when activating")
    void testTransitionToActive() {
        // When
        subscription.transitionTo(Subscription.SubscriptionState.ACTIVE);

        // Then
        assertThat(subscription.getState()).isEqualTo(Subscription.SubscriptionState.ACTIVE);
        assertThat(subscription.getActivationDate()).isNotNull();
    }

    @Test
    @DisplayName("transitionTo should not overwrite activation date if already set")
    void testTransitionToActivePreservesActivationDate() {
        // Given
        LocalDateTime originalDate = LocalDateTime.of(2025, 1, 1, 10, 0);
        subscription.setActivationDate(originalDate);
        subscription.setState(Subscription.SubscriptionState.SUSPENDED);

        // When
        subscription.transitionTo(Subscription.SubscriptionState.ACTIVE);

        // Then
        assertThat(subscription.getActivationDate()).isEqualTo(originalDate);
    }

    @Test
    @DisplayName("transitionTo SUSPENDED should change state")
    void testTransitionToSuspended() {
        // Given
        subscription.setState(Subscription.SubscriptionState.ACTIVE);

        // When
        subscription.transitionTo(Subscription.SubscriptionState.SUSPENDED);

        // Then
        assertThat(subscription.getState()).isEqualTo(Subscription.SubscriptionState.SUSPENDED);
    }

    // =========================================================================
    // State Check Tests
    // =========================================================================

    @Test
    @DisplayName("canActivate should return true for PENDING state")
    void testCanActivateFromPending() {
        // Given
        subscription.setState(Subscription.SubscriptionState.PENDING);

        // Then
        assertThat(subscription.canActivate()).isTrue();
    }

    @Test
    @DisplayName("canActivate should return true for SUSPENDED state")
    void testCanActivateFromSuspended() {
        // Given
        subscription.setState(Subscription.SubscriptionState.SUSPENDED);

        // Then
        assertThat(subscription.canActivate()).isTrue();
    }

    @Test
    @DisplayName("canActivate should return false for CANCELLED state")
    void testCannotActivateFromCancelled() {
        // Given
        subscription.setState(Subscription.SubscriptionState.CANCELLED);

        // Then
        assertThat(subscription.canActivate()).isFalse();
    }

    @Test
    @DisplayName("canActivate should return false for EXPIRED state")
    void testCannotActivateFromExpired() {
        // Given
        subscription.setState(Subscription.SubscriptionState.EXPIRED);

        // Then
        assertThat(subscription.canActivate()).isFalse();
    }

    @Test
    @DisplayName("canSuspend should return true only for ACTIVE state")
    void testCanSuspend() {
        // Given active state
        subscription.setState(Subscription.SubscriptionState.ACTIVE);
        assertThat(subscription.canSuspend()).isTrue();

        // Given pending state
        subscription.setState(Subscription.SubscriptionState.PENDING);
        assertThat(subscription.canSuspend()).isFalse();
    }

    @Test
    @DisplayName("canCancel should return true for ACTIVE or SUSPENDED states")
    void testCanCancel() {
        // Active can be cancelled
        subscription.setState(Subscription.SubscriptionState.ACTIVE);
        assertThat(subscription.canCancel()).isTrue();

        // Suspended can be cancelled
        subscription.setState(Subscription.SubscriptionState.SUSPENDED);
        assertThat(subscription.canCancel()).isTrue();

        // Pending cannot be cancelled
        subscription.setState(Subscription.SubscriptionState.PENDING);
        assertThat(subscription.canCancel()).isFalse();
    }

    @Test
    @DisplayName("isActive should return true only for ACTIVE state")
    void testIsActive() {
        // Given active state
        subscription.setState(Subscription.SubscriptionState.ACTIVE);
        assertThat(subscription.isActive()).isTrue();

        // Given other states
        subscription.setState(Subscription.SubscriptionState.PENDING);
        assertThat(subscription.isActive()).isFalse();
    }

    // =========================================================================
    // Recurring Cycle Tests (FR-015)
    // =========================================================================

    @Test
    @DisplayName("incrementRecurringCycle should increment counter")
    void testIncrementRecurringCycle() {
        // Given
        subscription.setRecurringCyclesCompleted(5);

        // When
        boolean expired = subscription.incrementRecurringCycle();

        // Then
        assertThat(subscription.getRecurringCyclesCompleted()).isEqualTo(6);
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("incrementRecurringCycle should auto-expire when max cycles reached (FR-015)")
    void testIncrementRecurringCycleAutoExpire() {
        // Given - one cycle before max
        subscription.setRecurringCyclesCompleted(11);
        subscription.setMaxRecurringCycles(12);

        // When
        boolean expired = subscription.incrementRecurringCycle();

        // Then
        assertThat(subscription.getRecurringCyclesCompleted()).isEqualTo(12);
        assertThat(expired).isTrue();
        assertThat(subscription.getState()).isEqualTo(Subscription.SubscriptionState.EXPIRED);
    }

    @Test
    @DisplayName("incrementRecurringCycle should not expire when maxRecurringCycles is null")
    void testIncrementRecurringCycleNoMax() {
        // Given - no max cycles set (unlimited)
        subscription.setRecurringCyclesCompleted(100);
        subscription.setMaxRecurringCycles(null);

        // When
        boolean expired = subscription.incrementRecurringCycle();

        // Then
        assertThat(subscription.getRecurringCyclesCompleted()).isEqualTo(101);
        assertThat(expired).isFalse();
        assertThat(subscription.getState()).isNotEqualTo(Subscription.SubscriptionState.EXPIRED);
    }

    @Test
    @DisplayName("incrementRecurringCycle should handle null recurringCyclesCompleted")
    void testIncrementRecurringCycleFromNull() {
        // Given
        subscription.setRecurringCyclesCompleted(null);

        // When
        subscription.incrementRecurringCycle();

        // Then
        assertThat(subscription.getRecurringCyclesCompleted()).isEqualTo(1);
    }

    @Test
    @DisplayName("hasReachedMaxCycles should return true when max reached")
    void testHasReachedMaxCycles() {
        // Given
        subscription.setRecurringCyclesCompleted(12);
        subscription.setMaxRecurringCycles(12);

        // Then
        assertThat(subscription.hasReachedMaxCycles()).isTrue();
    }

    @Test
    @DisplayName("hasReachedMaxCycles should return false when cycles remaining")
    void testHasNotReachedMaxCycles() {
        // Given
        subscription.setRecurringCyclesCompleted(5);
        subscription.setMaxRecurringCycles(12);

        // Then
        assertThat(subscription.hasReachedMaxCycles()).isFalse();
    }

    // =========================================================================
    // Renewal Date Calculation Tests (FR-017)
    // =========================================================================

    @Test
    @DisplayName("calculateRenewalDate should set renewal date for MONTHS cycle type (FR-017)")
    void testCalculateRenewalDateMonths() {
        // Given
        LocalDateTime activationDate = LocalDateTime.of(2025, 1, 15, 10, 0);
        subscription.setActivationDate(activationDate);
        subscription.setCycleLengthType(Subscription.CycleLengthType.MONTHS);
        subscription.setCycleLengthUnits(1);
        subscription.setRecurring(true);

        // When
        subscription.calculateRenewalDate();

        // Then
        assertThat(subscription.getRenewalDate()).isEqualTo(LocalDateTime.of(2025, 2, 15, 10, 0));
    }

    @Test
    @DisplayName("calculateRenewalDate should set renewal date for DAYS cycle type")
    void testCalculateRenewalDateDays() {
        // Given
        LocalDateTime activationDate = LocalDateTime.of(2025, 1, 1, 10, 0);
        subscription.setActivationDate(activationDate);
        subscription.setCycleLengthType(Subscription.CycleLengthType.DAYS);
        subscription.setCycleLengthUnits(30);
        subscription.setRecurring(true);

        // When
        subscription.calculateRenewalDate();

        // Then
        assertThat(subscription.getRenewalDate()).isEqualTo(LocalDateTime.of(2025, 1, 31, 10, 0));
    }

    @Test
    @DisplayName("calculateRenewalDate should set renewal date for WEEKS cycle type")
    void testCalculateRenewalDateWeeks() {
        // Given
        LocalDateTime activationDate = LocalDateTime.of(2025, 1, 1, 10, 0);
        subscription.setActivationDate(activationDate);
        subscription.setCycleLengthType(Subscription.CycleLengthType.WEEKS);
        subscription.setCycleLengthUnits(2);
        subscription.setRecurring(true);

        // When
        subscription.calculateRenewalDate();

        // Then
        assertThat(subscription.getRenewalDate()).isEqualTo(LocalDateTime.of(2025, 1, 15, 10, 0));
    }

    @Test
    @DisplayName("calculateRenewalDate should set renewal date for YEARS cycle type")
    void testCalculateRenewalDateYears() {
        // Given
        LocalDateTime activationDate = LocalDateTime.of(2025, 1, 1, 10, 0);
        subscription.setActivationDate(activationDate);
        subscription.setCycleLengthType(Subscription.CycleLengthType.YEARS);
        subscription.setCycleLengthUnits(1);
        subscription.setRecurring(true);

        // When
        subscription.calculateRenewalDate();

        // Then
        assertThat(subscription.getRenewalDate()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    @Test
    @DisplayName("calculateRenewalDate should set null for non-recurring subscription")
    void testCalculateRenewalDateNonRecurring() {
        // Given
        subscription.setRecurring(false);
        subscription.setActivationDate(LocalDateTime.now());

        // When
        subscription.calculateRenewalDate();

        // Then
        assertThat(subscription.getRenewalDate()).isNull();
    }

    @Test
    @DisplayName("calculateRenewalDate should set null when cycleLengthType is null")
    void testCalculateRenewalDateNullCycleType() {
        // Given
        subscription.setRecurring(true);
        subscription.setCycleLengthType(null);
        subscription.setActivationDate(LocalDateTime.now());

        // When
        subscription.calculateRenewalDate();

        // Then
        assertThat(subscription.getRenewalDate()).isNull();
    }

    @Test
    @DisplayName("calculateRenewalDate should use current time when activationDate is null")
    void testCalculateRenewalDateNoActivationDate() {
        // Given
        subscription.setActivationDate(null);
        subscription.setCycleLengthType(Subscription.CycleLengthType.DAYS);
        subscription.setCycleLengthUnits(1);
        subscription.setRecurring(true);

        LocalDateTime before = LocalDateTime.now();

        // When
        subscription.calculateRenewalDate();

        LocalDateTime after = LocalDateTime.now().plusDays(1);

        // Then
        assertThat(subscription.getRenewalDate()).isAfterOrEqualTo(before.plusDays(1));
        assertThat(subscription.getRenewalDate()).isBeforeOrEqualTo(after);
    }

    // =========================================================================
    // Builder and Default Values Tests
    // =========================================================================

    @Test
    @DisplayName("Builder should set default state to PENDING")
    void testDefaultState() {
        // Given
        Subscription newSub = Subscription.builder()
                .subscriberId("sub-id")
                .offerId("offer-id")
                .build();

        // Then
        assertThat(newSub.getState()).isEqualTo(Subscription.SubscriptionState.PENDING);
    }

    @Test
    @DisplayName("Builder should set default recurring to false")
    void testDefaultRecurring() {
        // Given
        Subscription newSub = Subscription.builder()
                .subscriberId("sub-id")
                .offerId("offer-id")
                .build();

        // Then
        assertThat(newSub.getRecurring()).isFalse();
    }

    @Test
    @DisplayName("Builder should set default recurringCyclesCompleted to 0")
    void testDefaultRecurringCyclesCompleted() {
        // Given
        Subscription newSub = Subscription.builder()
                .subscriberId("sub-id")
                .offerId("offer-id")
                .build();

        // Then
        assertThat(newSub.getRecurringCyclesCompleted()).isEqualTo(0);
    }

    // =========================================================================
    // Enum Tests
    // =========================================================================

    @Test
    @DisplayName("SubscriptionState enum should have all expected values")
    void testSubscriptionStateEnum() {
        assertThat(Subscription.SubscriptionState.values())
                .containsExactly(
                        Subscription.SubscriptionState.PENDING,
                        Subscription.SubscriptionState.ACTIVE,
                        Subscription.SubscriptionState.SUSPENDED,
                        Subscription.SubscriptionState.CANCELLED,
                        Subscription.SubscriptionState.EXPIRED
                );
    }

    @Test
    @DisplayName("CycleLengthType enum should have all expected values")
    void testCycleLengthTypeEnum() {
        assertThat(Subscription.CycleLengthType.values())
                .containsExactly(
                        Subscription.CycleLengthType.DAYS,
                        Subscription.CycleLengthType.WEEKS,
                        Subscription.CycleLengthType.MONTHS,
                        Subscription.CycleLengthType.YEARS
                );
    }
}
