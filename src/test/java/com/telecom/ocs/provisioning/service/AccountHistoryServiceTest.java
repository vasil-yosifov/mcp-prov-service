package com.telecom.ocs.provisioning.service;

import com.telecom.ocs.provisioning.models.AccountHistory;
import com.telecom.ocs.provisioning.repositories.AccountHistoryRepository;
import com.telecom.ocs.provisioning.services.AccountHistoryService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountHistoryService using Mockito.
 * 
 * Tests cover:
 * - T093: AccountHistoryService business logic
 * - CRUD operations with mocked repository
 * - Exception handling
 * - Validation logic
 */
@ExtendWith(MockitoExtension.class)
public class AccountHistoryServiceTest {

    @Mock
    private AccountHistoryRepository accountHistoryRepository;

    @InjectMocks
    private AccountHistoryService accountHistoryService;

    private AccountHistory sampleHistory;
    private String interactionId;
    private String entityId;

    @BeforeEach
    void setUp() {
        interactionId = UUID.randomUUID().toString();
        entityId = UUID.randomUUID().toString();
        
        sampleHistory = new AccountHistory();
        sampleHistory.setInteractionId(interactionId);
        sampleHistory.setEntityId(entityId);
        sampleHistory.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        sampleHistory.setCreationDate(LocalDateTime.now());
        sampleHistory.setDescription("Test interaction");
        sampleHistory.setStatus("COMPLETED");
    }

    /**
     * T093: Test createAccountHistory - successful creation
     */
    @Test
    void testCreateAccountHistory_Success() {
        when(accountHistoryRepository.existsById(anyString())).thenReturn(false);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(sampleHistory);

        AccountHistory result = accountHistoryService.createAccountHistory(sampleHistory);

        assertNotNull(result);
        assertEquals(interactionId, result.getInteractionId());
        assertEquals(entityId, result.getEntityId());
        
        verify(accountHistoryRepository, times(1)).existsById(interactionId);
        verify(accountHistoryRepository, times(1)).save(sampleHistory);
    }

    /**
     * T093: Test createAccountHistory - duplicate interaction ID
     */
    @Test
    void testCreateAccountHistory_DuplicateInteractionId() {
        when(accountHistoryRepository.existsById(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            accountHistoryService.createAccountHistory(sampleHistory);
        });

        verify(accountHistoryRepository, times(1)).existsById(interactionId);
        verify(accountHistoryRepository, never()).save(any());
    }

    /**
     * T093: Test createAccountHistory - null entity
     */
    @Test
    void testCreateAccountHistory_NullEntity() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountHistoryService.createAccountHistory(null);
        });

        verify(accountHistoryRepository, never()).existsById(anyString());
        verify(accountHistoryRepository, never()).save(any());
    }

    /**
     * T093: Test createAccountHistory - missing required fields
     */
    @Test
    void testCreateAccountHistory_MissingRequiredFields() {
        AccountHistory invalidHistory = new AccountHistory();
        invalidHistory.setInteractionId(interactionId);
        // Missing entityId, entityType, creationDate

        assertThrows(IllegalArgumentException.class, () -> {
            accountHistoryService.createAccountHistory(invalidHistory);
        });

        verify(accountHistoryRepository, never()).save(any());
    }

    /**
     * T093: Test getAccountHistoryByInteractionId - found
     */
    @Test
    void testGetAccountHistoryByInteractionId_Found() {
        when(accountHistoryRepository.findById(anyString())).thenReturn(Optional.of(sampleHistory));

        Optional<AccountHistory> result = accountHistoryService.getAccountHistoryByInteractionId(interactionId);

        assertTrue(result.isPresent());
        assertEquals(interactionId, result.get().getInteractionId());
        
        verify(accountHistoryRepository, times(1)).findById(interactionId);
    }

    /**
     * T093: Test getAccountHistoryByInteractionId - not found
     */
    @Test
    void testGetAccountHistoryByInteractionId_NotFound() {
        when(accountHistoryRepository.findById(anyString())).thenReturn(Optional.empty());

        Optional<AccountHistory> result = accountHistoryService.getAccountHistoryByInteractionId(interactionId);

        assertFalse(result.isPresent());
        
        verify(accountHistoryRepository, times(1)).findById(interactionId);
    }

    /**
     * T093: Test listAccountHistoryByEntityId - successful retrieval
     */
    @Test
    void testListAccountHistoryByEntityId_Success() {
        AccountHistory history1 = new AccountHistory();
        history1.setInteractionId(UUID.randomUUID().toString());
        history1.setEntityId(entityId);
        history1.setStartDateTime(LocalDateTime.now().minusHours(2));
        
        AccountHistory history2 = new AccountHistory();
        history2.setInteractionId(UUID.randomUUID().toString());
        history2.setEntityId(entityId);
        history2.setStartDateTime(LocalDateTime.now().minusHours(1));
        
        List<AccountHistory> historyList = Arrays.asList(history2, history1); // Newest first

        when(accountHistoryRepository.findByEntityIdOrderByStartDateTimeDesc(anyString()))
                .thenReturn(historyList);

        List<AccountHistory> result = accountHistoryService.listAccountHistoryByEntityId(entityId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(history2.getInteractionId(), result.get(0).getInteractionId());
        assertEquals(history1.getInteractionId(), result.get(1).getInteractionId());
        
        verify(accountHistoryRepository, times(1)).findByEntityIdOrderByStartDateTimeDesc(entityId);
    }

    /**
     * T093: Test listAccountHistoryByEntityId - empty list
     */
    @Test
    void testListAccountHistoryByEntityId_EmptyList() {
        when(accountHistoryRepository.findByEntityIdOrderByStartDateTimeDesc(anyString()))
                .thenReturn(Arrays.asList());

        List<AccountHistory> result = accountHistoryService.listAccountHistoryByEntityId(entityId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(accountHistoryRepository, times(1)).findByEntityIdOrderByStartDateTimeDesc(entityId);
    }

    /**
     * T093: Test updateAccountHistory - successful update
     */
    @Test
    void testUpdateAccountHistory_Success() {
        when(accountHistoryRepository.findById(anyString())).thenReturn(Optional.of(sampleHistory));
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(sampleHistory);

        AccountHistory updates = new AccountHistory();
        updates.setStatus("IN_PROGRESS");
        updates.setDescription("Updated description");

        AccountHistory result = accountHistoryService.updateAccountHistory(interactionId, updates);

        assertNotNull(result);
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals("Updated description", result.getDescription());
        
        verify(accountHistoryRepository, times(1)).findById(interactionId);
        verify(accountHistoryRepository, times(1)).save(any(AccountHistory.class));
    }

    /**
     * T093: Test updateAccountHistory - not found
     */
    @Test
    void testUpdateAccountHistory_NotFound() {
        when(accountHistoryRepository.findById(anyString())).thenReturn(Optional.empty());

        AccountHistory updates = new AccountHistory();
        updates.setStatus("COMPLETED");

        assertThrows(IllegalArgumentException.class, () -> {
            accountHistoryService.updateAccountHistory(interactionId, updates);
        });

        verify(accountHistoryRepository, times(1)).findById(interactionId);
        verify(accountHistoryRepository, never()).save(any());
    }

    /**
     * T093: Test updateAccountHistory - immutable fields protection
     */
    @Test
    void testUpdateAccountHistory_ImmutableFieldsProtection() {
        when(accountHistoryRepository.findById(anyString())).thenReturn(Optional.of(sampleHistory));
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(sampleHistory);

        AccountHistory updates = new AccountHistory();
        updates.setInteractionId("different-id"); // Should not change
        updates.setEntityId("different-entity-id"); // Should not change
        updates.setEntityType(AccountHistory.EntityType.GROUP); // Should not change
        updates.setStatus("UPDATED");

        AccountHistory result = accountHistoryService.updateAccountHistory(interactionId, updates);

        // Immutable fields should remain unchanged
        assertEquals(interactionId, result.getInteractionId());
        assertEquals(entityId, result.getEntityId());
        assertEquals(AccountHistory.EntityType.SUBSCRIBER, result.getEntityType());
        
        // Mutable field should be updated
        assertEquals("UPDATED", result.getStatus());
        
        verify(accountHistoryRepository, times(1)).save(any(AccountHistory.class));
    }

    /**
     * T093: Test deleteAccountHistory - successful deletion
     */
    @Test
    void testDeleteAccountHistory_Success() {
        when(accountHistoryRepository.existsById(anyString())).thenReturn(true);
        doNothing().when(accountHistoryRepository).deleteById(anyString());

        accountHistoryService.deleteAccountHistory(interactionId);

        verify(accountHistoryRepository, times(1)).existsById(interactionId);
        verify(accountHistoryRepository, times(1)).deleteById(interactionId);
    }

    /**
     * T093: Test deleteAccountHistory - not found
     */
    @Test
    void testDeleteAccountHistory_NotFound() {
        when(accountHistoryRepository.existsById(anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            accountHistoryService.deleteAccountHistory(interactionId);
        });

        verify(accountHistoryRepository, times(1)).existsById(interactionId);
        verify(accountHistoryRepository, never()).deleteById(anyString());
    }

    /**
     * T093: Test validation of entity types
     */
    @Test
    void testValidateEntityType_ValidTypes() {
        AccountHistory.EntityType[] validTypes = {
            AccountHistory.EntityType.SUBSCRIBER,
            AccountHistory.EntityType.GROUP,
            AccountHistory.EntityType.ACCOUNT
        };

        for (AccountHistory.EntityType entityType : validTypes) {
            AccountHistory history = new AccountHistory();
            history.setInteractionId(UUID.randomUUID().toString());
            history.setEntityId(entityId);
            history.setEntityType(entityType);
            history.setCreationDate(LocalDateTime.now());

            when(accountHistoryRepository.existsById(anyString())).thenReturn(false);
            when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(history);

            AccountHistory result = accountHistoryService.createAccountHistory(history);
            assertEquals(entityType, result.getEntityType());
        }
    }

    /**
     * T093: Test validation of entity types - null type (invalid)
     */
    @Test
    void testValidateEntityType_InvalidType() {
        AccountHistory history = new AccountHistory();
        history.setInteractionId(interactionId);
        history.setEntityId(entityId);
        history.setEntityType(null);  // null is now the invalid case with enum
        history.setCreationDate(LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () -> {
            accountHistoryService.createAccountHistory(history);
        });

        verify(accountHistoryRepository, never()).save(any());
    }

    /**
     * T093: Test handling of optional fields
     */
    @Test
    void testHandlingOfOptionalFields() {
        AccountHistory minimalHistory = new AccountHistory();
        minimalHistory.setInteractionId(interactionId);
        minimalHistory.setEntityId(entityId);
        minimalHistory.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        minimalHistory.setCreationDate(LocalDateTime.now());

        when(accountHistoryRepository.existsById(anyString())).thenReturn(false);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(minimalHistory);

        AccountHistory result = accountHistoryService.createAccountHistory(minimalHistory);

        assertNotNull(result);
        assertNull(result.getDescription());
        assertNull(result.getDirection());
        assertNull(result.getReason());
        assertNull(result.getStatus());
        
        verify(accountHistoryRepository, times(1)).save(minimalHistory);
    }

    /**
     * T093: Test repository interaction patterns
     */
    @Test
    void testRepositoryInteractionPatterns() {
        // Test that service doesn't call repository unnecessarily
        reset(accountHistoryRepository);

        when(accountHistoryRepository.findById(anyString())).thenReturn(Optional.empty());

        Optional<AccountHistory> result = accountHistoryService.getAccountHistoryByInteractionId(interactionId);

        assertFalse(result.isPresent());
        verify(accountHistoryRepository, times(1)).findById(interactionId);
        verifyNoMoreInteractions(accountHistoryRepository);
    }
}
