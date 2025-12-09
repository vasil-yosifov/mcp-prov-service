package com.telecom.ocs.provisioning.service;

import com.telecom.ocs.provisioning.exceptions.DuplicateResourceException;
import com.telecom.ocs.provisioning.exceptions.ResourceNotFoundException;
import com.telecom.ocs.provisioning.models.Subscriber;
import com.telecom.ocs.provisioning.repositories.SubscriberRepository;
import com.telecom.ocs.provisioning.services.SubscriberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
 * Unit tests for SubscriberService.
 * Uses Mockito to mock repository dependencies and test business logic in isolation.
 * 
 * Tests:
 * - T046: CRUD operations (create, read, update, delete)
 * - T046: State transition logic
 * - T046: Cascade delete handling
 * - T046: Duplicate msisdn validation
 * - T046: Not found scenarios
 */
@ExtendWith(MockitoExtension.class)
class SubscriberServiceTest {

    @Mock
    private SubscriberRepository subscriberRepository;

    @InjectMocks
    private SubscriberService subscriberService;

    private Subscriber testSubscriber;
    private String testSubscriberId;

    @BeforeEach
    void setUp() {
        testSubscriberId = UUID.randomUUID().toString();
        
        testSubscriber = new Subscriber();
        testSubscriber.setSubscriberId(testSubscriberId);
        testSubscriber.setMsisdn("43664123456789");
        testSubscriber.setImsi("214010123456789");
        testSubscriber.setFirstName("John");
        testSubscriber.setLastName("Doe");
        testSubscriber.setDateOfBirth(LocalDate.of(1990, 1, 15));
        testSubscriber.setEmail("john.doe@example.com");
        testSubscriber.setContactNumber("436641234567");
        testSubscriber.setBillingCycle("1");
        testSubscriber.setBillingStreet("123 Main St");
        testSubscriber.setBillingCity("Vienna");
        testSubscriber.setBillingCountry("Austria");
        testSubscriber.setState(Subscriber.SubscriberState.PRE_PROVISIONED);
        testSubscriber.setCreationDate(LocalDateTime.now());
        testSubscriber.setLastModifiedDate(LocalDateTime.now());
        testSubscriber.setVersion(0L);
    }

    /**
     * T046: Test creating a new subscriber
     * Expected: Subscriber is saved with generated ID and timestamps
     */
    @Test
    void testCreateSubscriber() {
        // Given
        when(subscriberRepository.findByMsisdn(anyString())).thenReturn(Optional.empty());
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(testSubscriber);

        // When
        Subscriber created = subscriberService.createSubscriber(testSubscriber);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getSubscriberId()).isEqualTo(testSubscriberId);
        assertThat(created.getMsisdn()).isEqualTo("43664123456789");
        assertThat(created.getState()).isEqualTo(Subscriber.SubscriberState.PRE_PROVISIONED);
        
        verify(subscriberRepository).findByMsisdn("43664123456789");
        verify(subscriberRepository).save(testSubscriber);
    }

    /**
     * T046: Test creating subscriber with duplicate msisdn
     * Expected: DuplicateResourceException thrown
     */
    @Test
    void testCreateSubscriberDuplicateMsisdn() {
        // Given
        when(subscriberRepository.findByMsisdn(anyString())).thenReturn(Optional.of(testSubscriber));

        // When / Then
        assertThatThrownBy(() -> subscriberService.createSubscriber(testSubscriber))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("msisdn");

        verify(subscriberRepository).findByMsisdn("43664123456789");
        verify(subscriberRepository, never()).save(any());
    }

    /**
     * T046: Test retrieving subscriber by ID
     * Expected: Subscriber is found and returned
     */
    @Test
    void testGetSubscriberById() {
        // Given
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));

        // When
        Subscriber found = subscriberService.getSubscriberById(testSubscriberId);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getSubscriberId()).isEqualTo(testSubscriberId);
        assertThat(found.getMsisdn()).isEqualTo("43664123456789");
        
        verify(subscriberRepository).findById(testSubscriberId);
    }

    /**
     * T046: Test retrieving non-existent subscriber
     * Expected: ResourceNotFoundException thrown
     */
    @Test
    void testGetSubscriberByIdNotFound() {
        // Given
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> subscriberService.getSubscriberById("non-existent-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Subscriber")
                .hasMessageContaining("non-existent-id");

        verify(subscriberRepository).findById("non-existent-id");
    }

    /**
     * T046: Test looking up subscriber by msisdn
     * Expected: Subscriber is found and returned
     */
    @Test
    void testLookupSubscriberByMsisdn() {
        // Given
        when(subscriberRepository.findByMsisdn("43664123456789")).thenReturn(Optional.of(testSubscriber));

        // When
        Optional<Subscriber> found = subscriberService.lookupByMsisdn("43664123456789");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get()).isNotNull();
        assertThat(found.get().getMsisdn()).isEqualTo("43664123456789");
        
        verify(subscriberRepository).findByMsisdn("43664123456789");
    }

    /**
     * T046: Test looking up subscriber by imsi
     * Expected: Subscriber is found and returned
     */
    @Test
    void testLookupSubscriberByImsi() {
        // Given
        when(subscriberRepository.findByImsi("214010123456789")).thenReturn(Optional.of(testSubscriber));

        // When
        Optional<Subscriber> found = subscriberService.lookupByImsi("214010123456789");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get()).isNotNull();
        assertThat(found.get().getImsi()).isEqualTo("214010123456789");
        
        verify(subscriberRepository).findByImsi("214010123456789");
    }

    /**
     * T046: Test looking up subscriber by name
     * Expected: Matching subscribers are returned
     */
    @Test
    void testLookupSubscriberByName() {
        // Given
        List<Subscriber> subscribers = Arrays.asList(testSubscriber);
        when(subscriberRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(subscribers);

        // When
        List<Subscriber> found = subscriberService.lookupByName("John", "Doe");

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getFirstName()).isEqualTo("John");
        assertThat(found.get(0).getLastName()).isEqualTo("Doe");
        
        verify(subscriberRepository).findByFirstNameAndLastName("John", "Doe");
    }

    /**
     * T046: Test updating subscriber state
     * Expected: State transition is recorded with previousState and lastTransitionDate
     */
    @Test
    void testUpdateSubscriberState() {
        // Given
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));
        
        Subscriber updatedSubscriber = new Subscriber();
        updatedSubscriber.setSubscriberId(testSubscriberId);
        updatedSubscriber.setMsisdn(testSubscriber.getMsisdn());
        updatedSubscriber.setState(Subscriber.SubscriberState.ACTIVE);
        updatedSubscriber.setPreviousState(Subscriber.SubscriberState.PRE_PROVISIONED);
        updatedSubscriber.setLastTransitionDate(LocalDateTime.now());
        
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(updatedSubscriber);

        // When
        Subscriber updated = subscriberService.updateSubscriberState(testSubscriberId, Subscriber.SubscriberState.ACTIVE);

        // Then
        assertThat(updated.getState()).isEqualTo(Subscriber.SubscriberState.ACTIVE);
        assertThat(updated.getPreviousState()).isEqualTo(Subscriber.SubscriberState.PRE_PROVISIONED);
        assertThat(updated.getLastTransitionDate()).isNotNull();
        
        verify(subscriberRepository).findById(testSubscriberId);
        verify(subscriberRepository).save(any(Subscriber.class));
    }

    /**
     * T046: Test updating subscriber state for non-existent subscriber
     * Expected: ResourceNotFoundException thrown
     */
    @Test
    void testUpdateSubscriberStateNotFound() {
        // Given
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> subscriberService.updateSubscriberState("non-existent-id", Subscriber.SubscriberState.ACTIVE))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(subscriberRepository).findById("non-existent-id");
        verify(subscriberRepository, never()).save(any());
    }

    /**
     * T046: Test updating subscriber fields (PATCH operation)
     * Expected: Specified fields are updated
     */
    @Test
    void testUpdateSubscriberFields() {
        // Given
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(testSubscriber);

        // When
        testSubscriber.setEmail("newemail@example.com");
        Subscriber updated = subscriberService.updateSubscriber(testSubscriberId, testSubscriber);

        // Then
        assertThat(updated.getEmail()).isEqualTo("newemail@example.com");
        
        verify(subscriberRepository).findById(testSubscriberId);
        verify(subscriberRepository).save(any(Subscriber.class));
    }

    /**
     * T046: Test deleting subscriber
     * Expected: Subscriber is deleted with cascade handling
     */
    @Test
    void testDeleteSubscriber() {
        // Given
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));
        doNothing().when(subscriberRepository).delete(any(Subscriber.class));

        // When
        subscriberService.deleteSubscriber(testSubscriberId);

        // Then
        verify(subscriberRepository).findById(testSubscriberId);
        verify(subscriberRepository).delete(testSubscriber);
    }

    /**
     * T046: Test deleting non-existent subscriber
     * Expected: ResourceNotFoundException thrown
     */
    @Test
    void testDeleteSubscriberNotFound() {
        // Given
        when(subscriberRepository.findById(anyString())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> subscriberService.deleteSubscriber("non-existent-id"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(subscriberRepository).findById("non-existent-id");
        verify(subscriberRepository, never()).delete(any());
    }

    /**
     * T046: Test cascade delete handling
     * Expected: Deleting subscriber triggers cascade delete of related entities
     * Note: Actual cascade is handled by JPA, service just calls delete
     */
    @Test
    void testCascadeDelete() {
        // Given
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));
        doNothing().when(subscriberRepository).delete(any(Subscriber.class));

        // When
        subscriberService.deleteSubscriber(testSubscriberId);

        // Then
        verify(subscriberRepository).delete(testSubscriber);
        // JPA cascade configuration will handle deletion of subscriptions, notifications, timers
    }

    /**
     * T046: Test retrieving all subscribers
     * Expected: All subscribers are returned
     */
    @Test
    void testGetAllSubscribers() {
        // Given
        Subscriber subscriber2 = new Subscriber();
        subscriber2.setSubscriberId(UUID.randomUUID().toString());
        subscriber2.setMsisdn("43664987654321");
        subscriber2.setState(Subscriber.SubscriberState.ACTIVE);
        
        List<Subscriber> subscribers = Arrays.asList(testSubscriber, subscriber2);
        when(subscriberRepository.findAll()).thenReturn(subscribers);

        // When
        List<Subscriber> all = subscriberService.getAllSubscribers();

        // Then
        assertThat(all).hasSize(2);
        assertThat(all).containsExactly(testSubscriber, subscriber2);
        
        verify(subscriberRepository).findAll();
    }

    /**
     * T046: Test state transition validation
     * Expected: Only valid state transitions are allowed
     */
    @Test
    void testStateTransitionValidation() {
        // Given
        testSubscriber.setState(Subscriber.SubscriberState.TERMINATED);
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));

        // When / Then - Attempting to transition from TERMINATED to ACTIVE should be invalid
        assertThatThrownBy(() -> subscriberService.updateSubscriberState(testSubscriberId, Subscriber.SubscriberState.ACTIVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid state transition");

        verify(subscriberRepository).findById(testSubscriberId);
    }

    /**
     * T046: Test optimistic locking handling
     * Expected: Service handles version conflicts appropriately
     */
    @Test
    void testOptimisticLockingConflict() {
        // Given
        testSubscriber.setVersion(1L);
        when(subscriberRepository.findById(testSubscriberId)).thenReturn(Optional.of(testSubscriber));
        
        Subscriber staleSubscriber = new Subscriber();
        staleSubscriber.setSubscriberId(testSubscriberId);
        staleSubscriber.setVersion(0L); // Stale version
        
        when(subscriberRepository.save(any(Subscriber.class)))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException("Subscriber", testSubscriberId));

        // When / Then
        assertThatThrownBy(() -> subscriberService.updateSubscriber(testSubscriberId, staleSubscriber))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);

        verify(subscriberRepository).save(any(Subscriber.class));
    }
}
