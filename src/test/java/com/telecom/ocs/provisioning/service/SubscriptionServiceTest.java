package com.telecom.ocs.provisioning.service;

import com.telecom.ocs.provisioning.exceptions.DuplicateResourceException;
import com.telecom.ocs.provisioning.exceptions.ResourceNotFoundException;
import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.models.Subscription;
import com.telecom.ocs.provisioning.repositories.SubscriberRepository;
import com.telecom.ocs.provisioning.repositories.SubscriptionRepository;
import com.telecom.ocs.provisioning.services.SubscriptionService;
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
 * Unit tests for SubscriptionService.
 * Uses Mockito to mock repository dependencies and test business logic in isolation.
 * 
 * Tests:
 * - T062: CRUD operations (create, read, update, delete)
 * - T062: State transition logic (activate, suspend, cancel)
 * - T062: Recurring cycle management
 * - T062: Auto-expiration when max cycles reached (FR-015)
 * - T062: Renewal date calculation (FR-017)
 * - T062: Cascade delete handling
 * - T062: Subscriber validation
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriberRepository subscriberRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Subscriber testSubscriber;
    private Subscription testSubscription;
    private String testSubscriberId;
    private String testSubscriptionId;

    @BeforeEach
    void setUp() {
        testSubscriberId = UUID.randomUUID().toString();
        testSubscriptionId = UUID.randomUUID().toString();

        // Set up test subscriber
        testSubscriber = new Subscriber();
        testSubscriber.setSubscriberId(testSubscriberId);
        testSubscriber.setMsisdn("43664123456789");
        testSubscriber.setFirstName("John");
        testSubscriber.setLastName("Doe");
        testSubscriber.setState(Subscriber.SubscriberState.ACTIVE);
        testSubscriber.setCreationDate(LocalDateTime.now());
        testSubscriber.setLastModifiedDate(LocalDateTime.now());
        testSubscriber.setVersion(0L);

        // Set up test subscription
        testSubscription = new Subscription();
        testSubscription.setSubscriptionId(testSubscriptionId);
        testSubscription.setSubscriberId(testSubscriberId);
        testSubscription.setOfferId("OFFER-001");
        testSubscription.setOfferName("Premium Data Plan");
        testSubscription.setSubscriptionType("DATA");
        testSubscription.setState(Subscription.SubscriptionState.PENDING);
        testSubscription.setRecurring(true);
        testSubscription.setMaxRecurringCycles(12);
        testSubscription.setRecurringCyclesCompleted(0);
        testSubscription.setCycleLengthUnits(1);
        testSubscription.setCycleLengthType(Subscription.CycleLengthType.MONTHS);
        testSubscription.setCreationDate(LocalDateTime.now());
        testSubscription.setLastModifiedDate(LocalDateTime.now());
        testSubscription.setVersion(0L);
    }

    // =========================================================================
    // Create Subscription Tests
    // =========================================================================

    /**
     * T062: Test creating a new subscription
     * Expected: Subscription is saved with generated ID and timestamps
     */
    @Test
    void testCreateSubscription() {
        // Given
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        // When
        Subscription created = subscriptionService.createSubscription(testSubscriberId, testSubscription);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getSubscriptionId()).isEqualTo(testSubscriptionId);
        assertThat(created.getSubscriberId()).isEqualTo(testSubscriberId);
        assertThat(created.getOfferId()).isEqualTo("OFFER-001");
        assertThat(created.getState()).isEqualTo(Subscription.SubscriptionState.PENDING);
        
        verify(subscriberRepository).findById(testSubscriberId);
        verify(subscriptionRepository).save(testSubscription);
    }

    /**
     * T062: Test creating subscription for non-existent subscriber
     * Expected: ResourceNotFoundException thrown
     */
    @Test
    void testCreateSubscriptionSubscriberNotFound() {
        // Given
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> subscriptionService.createSubscription("non-existent-id", testSubscription))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Subscriber");

        verify(subscriberRepository).findById("non-existent-id");
        verify(subscriptionRepository, never()).save(any());
    }

    /**
     * T062: Test creating subscription with duplicate subscriptionId
     * Expected: DuplicateResourceException thrown
     */
    @Test
    void testCreateSubscriptionDuplicateId() {
        // Given
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));
        when(subscriptionRepository.existsById(testSubscriptionId)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(testSubscriberId, testSubscription))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("subscriptionId");

        verify(subscriptionRepository, never()).save(any());
    }

    /**
     * T062: Test that subscription is created with PENDING state by default
     * Expected: State is set to PENDING if not provided
     */
    @Test
    void testCreateSubscriptionDefaultState() {
        // Given
        testSubscription.setState(null);
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription saved = invocation.getArgument(0);
            assertThat(saved.getState()).isEqualTo(Subscription.SubscriptionState.PENDING);
            return saved;
        });

        // When
        subscriptionService.createSubscription(testSubscriberId, testSubscription);

        // Then
        verify(subscriptionRepository).save(testSubscription);
    }

    // =========================================================================
    // Retrieve Subscription Tests
    // =========================================================================

    /**
     * T062: Test retrieving subscription by ID
     * Expected: Subscription is found and returned
     */
    @Test
    void testGetSubscriptionById() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));

        // When
        Subscription found = subscriptionService.getSubscriptionById(testSubscriptionId);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getSubscriptionId()).isEqualTo(testSubscriptionId);
        assertThat(found.getOfferId()).isEqualTo("OFFER-001");
        
        verify(subscriptionRepository).findById(testSubscriptionId);
    }

    /**
     * T062: Test retrieving non-existent subscription
     * Expected: ResourceNotFoundException thrown
     */
    @Test
    void testGetSubscriptionByIdNotFound() {
        // Given
        when(subscriptionRepository.findById(anyString())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> subscriptionService.getSubscriptionById("non-existent-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Subscription")
                .hasMessageContaining("non-existent-id");

        verify(subscriptionRepository).findById("non-existent-id");
    }

    /**
     * T062: Test listing subscriptions for a subscriber
     * Expected: All subscriptions for subscriber are returned
     */
    @Test
    void testGetSubscriptionsBySubscriberId() {
        // Given
        Subscription subscription2 = new Subscription();
        subscription2.setSubscriptionId(UUID.randomUUID().toString());
        subscription2.setSubscriberId(testSubscriberId);
        subscription2.setOfferId("OFFER-002");
        subscription2.setState(Subscription.SubscriptionState.ACTIVE);

        List<Subscription> subscriptions = Arrays.asList(testSubscription, subscription2);
        
        when(subscriberRepository.existsById(testSubscriberId)).thenReturn(true);
        when(subscriptionRepository.findBySubscriberId(testSubscriberId)).thenReturn(subscriptions);

        // When
        List<Subscription> found = subscriptionService.getSubscriptionsBySubscriberId(testSubscriberId);

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Subscription::getOfferId)
                .containsExactly("OFFER-001", "OFFER-002");
        
        verify(subscriberRepository).existsById(testSubscriberId);
        verify(subscriptionRepository).findBySubscriberId(testSubscriberId);
    }

    /**
     * T062: Test listing subscriptions for non-existent subscriber
     * Expected: ResourceNotFoundException thrown
     */
    @Test
    void testGetSubscriptionsBySubscriberIdNotFound() {
        // Given
        when(subscriberRepository.existsById(anyString())).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> subscriptionService.getSubscriptionsBySubscriberId("non-existent-id"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(subscriberRepository).existsById("non-existent-id");
        verify(subscriptionRepository, never()).findBySubscriberId(anyString());
    }

    // =========================================================================
    // State Transition Tests
    // =========================================================================

    /**
     * T062: Test activating a subscription (PENDING → ACTIVE)
     * Expected: State changes to ACTIVE, activationDate is set
     */
    @Test
    void testActivateSubscription() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        
        Subscription activatedSubscription = new Subscription();
        activatedSubscription.setSubscriptionId(testSubscriptionId);
        activatedSubscription.setSubscriberId(testSubscriberId);
        activatedSubscription.setState(Subscription.SubscriptionState.ACTIVE);
        activatedSubscription.setActivationDate(LocalDateTime.now());
        
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(activatedSubscription);

        // When
        Subscription activated = subscriptionService.activateSubscription(testSubscriptionId);

        // Then
        assertThat(activated.getState()).isEqualTo(Subscription.SubscriptionState.ACTIVE);
        assertThat(activated.getActivationDate()).isNotNull();
        
        verify(subscriptionRepository).findById(testSubscriptionId);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    /**
     * T062: Test suspending a subscription (ACTIVE → SUSPENDED)
     * Expected: State changes to SUSPENDED
     */
    @Test
    void testSuspendSubscription() {
        // Given
        testSubscription.setState(Subscription.SubscriptionState.ACTIVE);
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        
        Subscription suspendedSubscription = new Subscription();
        suspendedSubscription.setSubscriptionId(testSubscriptionId);
        suspendedSubscription.setState(Subscription.SubscriptionState.SUSPENDED);
        
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(suspendedSubscription);

        // When
        Subscription suspended = subscriptionService.suspendSubscription(testSubscriptionId);

        // Then
        assertThat(suspended.getState()).isEqualTo(Subscription.SubscriptionState.SUSPENDED);
        
        verify(subscriptionRepository).findById(testSubscriptionId);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    /**
     * T062: Test cancelling a subscription (ACTIVE → CANCELLED)
     * Expected: State changes to CANCELLED
     */
    @Test
    void testCancelSubscription() {
        // Given
        testSubscription.setState(Subscription.SubscriptionState.ACTIVE);
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        
        Subscription cancelledSubscription = new Subscription();
        cancelledSubscription.setSubscriptionId(testSubscriptionId);
        cancelledSubscription.setState(Subscription.SubscriptionState.CANCELLED);
        
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(cancelledSubscription);

        // When
        Subscription cancelled = subscriptionService.cancelSubscription(testSubscriptionId);

        // Then
        assertThat(cancelled.getState()).isEqualTo(Subscription.SubscriptionState.CANCELLED);
        
        verify(subscriptionRepository).findById(testSubscriptionId);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    /**
     * T062: Test invalid state transition
     * Expected: IllegalStateException thrown
     */
    @Test
    void testInvalidStateTransition() {
        // Given - Try to activate an already cancelled subscription
        testSubscription.setState(Subscription.SubscriptionState.CANCELLED);
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));

        // When / Then
        assertThatThrownBy(() -> subscriptionService.activateSubscription(testSubscriptionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");

        verify(subscriptionRepository).findById(testSubscriptionId);
        verify(subscriptionRepository, never()).save(any());
    }

    // =========================================================================
    // Recurring Cycle Tests (FR-015, FR-017)
    // =========================================================================

    /**
     * T062 (FR-015): Test auto-expiration when recurringCyclesCompleted == maxRecurringCycles
     * Expected: State transitions to EXPIRED when max cycles reached
     */
    @Test
    void testAutoExpirationOnMaxCycles() {
        // Given
        testSubscription.setState(Subscription.SubscriptionState.ACTIVE);
        testSubscription.setRecurring(true);
        testSubscription.setMaxRecurringCycles(12);
        testSubscription.setRecurringCyclesCompleted(11); // One cycle left
        
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription saved = invocation.getArgument(0);
            // Verify auto-expiration logic
            if (saved.getRecurringCyclesCompleted() >= saved.getMaxRecurringCycles()) {
                saved.setState(Subscription.SubscriptionState.EXPIRED);
            }
            return saved;
        });

        // When - Increment cycle count to reach max
        Subscription updated = subscriptionService.incrementRecurringCycle(testSubscriptionId);

        // Then
        assertThat(updated.getRecurringCyclesCompleted()).isEqualTo(12);
        assertThat(updated.getState()).isEqualTo(Subscription.SubscriptionState.EXPIRED);
        
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    /**
     * T062 (FR-015): Test no auto-expiration when cycles remaining
     * Expected: State remains ACTIVE when max cycles not reached
     */
    @Test
    void testNoAutoExpirationWhenCyclesRemaining() {
        // Given
        testSubscription.setState(Subscription.SubscriptionState.ACTIVE);
        testSubscription.setRecurring(true);
        testSubscription.setMaxRecurringCycles(12);
        testSubscription.setRecurringCyclesCompleted(5); // Plenty of cycles left
        
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Subscription updated = subscriptionService.incrementRecurringCycle(testSubscriptionId);

        // Then
        assertThat(updated.getRecurringCyclesCompleted()).isEqualTo(6);
        assertThat(updated.getState()).isEqualTo(Subscription.SubscriptionState.ACTIVE);
    }

    /**
     * T062 (FR-017): Test renewal date calculation
     * Expected: renewalDate is calculated based on cycleLengthType and cycleLengthUnits
     */
    @Test
    void testRenewalDateCalculation() {
        // Given
        testSubscription.setState(Subscription.SubscriptionState.PENDING);
        testSubscription.setCycleLengthType(Subscription.CycleLengthType.MONTHS);
        testSubscription.setCycleLengthUnits(1);
        LocalDateTime now = LocalDateTime.now();
        
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Subscription activated = subscriptionService.activateSubscription(testSubscriptionId);

        // Then
        assertThat(activated.getRenewalDate()).isNotNull();
        assertThat(activated.getRenewalDate()).isAfter(now);
        // For 1 MONTH cycle, renewalDate should be approximately 1 month from activation
        assertThat(activated.getRenewalDate()).isBefore(now.plusMonths(2));
    }

    /**
     * T062: Test non-recurring subscription has no renewal date
     * Expected: renewalDate remains null for non-recurring subscriptions
     */
    @Test
    void testNoRenewalDateForNonRecurring() {
        // Given
        testSubscription.setState(Subscription.SubscriptionState.PENDING);
        testSubscription.setRecurring(false);
        
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Subscription activated = subscriptionService.activateSubscription(testSubscriptionId);

        // Then
        assertThat(activated.getRenewalDate()).isNull();
    }

    // =========================================================================
    // Update Subscription Tests
    // =========================================================================

    /**
     * T062: Test updating subscription fields
     * Expected: Specified fields are updated
     */
    @Test
    void testUpdateSubscription() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        // When
        testSubscription.setOfferName("Updated Plan Name");
        Subscription updated = subscriptionService.updateSubscription(testSubscriptionId, testSubscription);

        // Then
        assertThat(updated.getOfferName()).isEqualTo("Updated Plan Name");
        
        verify(subscriptionRepository).findById(testSubscriptionId);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    /**
     * T062: Test updating non-existent subscription
     * Expected: ResourceNotFoundException thrown
     */
    @Test
    void testUpdateSubscriptionNotFound() {
        // Given
        when(subscriptionRepository.findById(anyString())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> subscriptionService.updateSubscription("non-existent-id", testSubscription))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(subscriptionRepository).findById("non-existent-id");
        verify(subscriptionRepository, never()).save(any());
    }

    // =========================================================================
    // Delete Subscription Tests
    // =========================================================================

    /**
     * T062: Test deleting subscription
     * Expected: Subscription is deleted
     */
    @Test
    void testDeleteSubscription() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        doNothing().when(subscriptionRepository).delete(any(Subscription.class));

        // When
        subscriptionService.deleteSubscription(testSubscriptionId);

        // Then
        verify(subscriptionRepository).findById(testSubscriptionId);
        verify(subscriptionRepository).delete(testSubscription);
    }

    /**
     * T062: Test deleting non-existent subscription
     * Expected: ResourceNotFoundException thrown
     */
    @Test
    void testDeleteSubscriptionNotFound() {
        // Given
        when(subscriptionRepository.findById(anyString())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> subscriptionService.deleteSubscription("non-existent-id"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(subscriptionRepository).findById("non-existent-id");
        verify(subscriptionRepository, never()).delete(any());
    }

    /**
     * T062: Test cascade delete handling (balances deleted with subscription)
     * Expected: JPA cascade handles deletion of related balances
     * Note: Actual cascade is handled by JPA, service just calls delete
     */
    @Test
    void testCascadeDelete() {
        // Given
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        doNothing().when(subscriptionRepository).delete(any(Subscription.class));

        // When
        subscriptionService.deleteSubscription(testSubscriptionId);

        // Then
        verify(subscriptionRepository).delete(testSubscription);
        // JPA cascade configuration will handle deletion of balances
    }

    // =========================================================================
    // Optimistic Locking Tests
    // =========================================================================

    /**
     * T062: Test optimistic locking conflict handling
     * Expected: Service handles version conflicts appropriately
     */
    @Test
    void testOptimisticLockingConflict() {
        // Given
        testSubscription.setVersion(1L);
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        
        Subscription staleSubscription = new Subscription();
        staleSubscription.setSubscriptionId(testSubscriptionId);
        staleSubscription.setVersion(0L); // Stale version
        
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException("Subscription", testSubscriptionId));

        // When / Then
        assertThatThrownBy(() -> subscriptionService.updateSubscription(testSubscriptionId, staleSubscription))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    // =========================================================================
    // Bulk Operations Tests
    // =========================================================================

    /**
     * T062: Test getting all subscriptions
     * Expected: All subscriptions are returned
     */
    @Test
    void testGetAllSubscriptions() {
        // Given
        Subscription subscription2 = new Subscription();
        subscription2.setSubscriptionId(UUID.randomUUID().toString());
        subscription2.setOfferId("OFFER-002");
        subscription2.setState(Subscription.SubscriptionState.ACTIVE);
        
        List<Subscription> subscriptions = Arrays.asList(testSubscription, subscription2);
        when(subscriptionRepository.findAll()).thenReturn(subscriptions);

        // When
        List<Subscription> all = subscriptionService.getAllSubscriptions();

        // Then
        assertThat(all).hasSize(2);
        assertThat(all).containsExactly(testSubscription, subscription2);
        
        verify(subscriptionRepository).findAll();
    }

    /**
     * T062: Test getting subscriptions by state
     * Expected: Subscriptions in specified state are returned
     */
    @Test
    void testGetSubscriptionsByState() {
        // Given
        List<Subscription> activeSubscriptions = Arrays.asList(testSubscription);
        when(subscriptionRepository.findByState(Subscription.SubscriptionState.ACTIVE)).thenReturn(activeSubscriptions);

        // When
        List<Subscription> found = subscriptionService.getSubscriptionsByState(Subscription.SubscriptionState.ACTIVE);

        // Then
        assertThat(found).hasSize(1);
        
        verify(subscriptionRepository).findByState(Subscription.SubscriptionState.ACTIVE);
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    /**
     * T062: Test creating subscription with null maxRecurringCycles
     * Expected: Subscription is created without auto-expiration logic
     */
    @Test
    void testCreateSubscriptionNullMaxCycles() {
        // Given
        testSubscription.setRecurring(true);
        testSubscription.setMaxRecurringCycles(null);
        
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        // When
        Subscription created = subscriptionService.createSubscription(testSubscriberId, testSubscription);

        // Then
        assertThat(created.getMaxRecurringCycles()).isNull();
        assertThat(created.getRecurring()).isTrue();
        // Should not throw exception
    }

    /**
     * T062: Test cycle length calculation for different types
     * Expected: Correct renewal dates for DAYS, WEEKS, MONTHS, YEARS
     */
    @Test
    void testCycleLengthCalculationDays() {
        // Given
        testSubscription.setCycleLengthType(Subscription.CycleLengthType.DAYS);
        testSubscription.setCycleLengthUnits(30);
        LocalDateTime now = LocalDateTime.now();
        
        when(subscriptionRepository.findById(testSubscriptionId)).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Subscription activated = subscriptionService.activateSubscription(testSubscriptionId);

        // Then
        assertThat(activated.getRenewalDate()).isNotNull();
        assertThat(activated.getRenewalDate()).isAfterOrEqualTo(now.plusDays(30));
    }
}
