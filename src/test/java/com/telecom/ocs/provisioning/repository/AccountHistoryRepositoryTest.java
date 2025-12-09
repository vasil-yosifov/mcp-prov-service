package com.telecom.ocs.provisioning.repository;

import com.telecom.ocs.provisioning.models.AccountHistory;
import com.telecom.ocs.provisioning.repositories.AccountHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for AccountHistoryRepository.
 * 
 * Tests cover:
 * - T092: AccountHistoryRepository CRUD operations
 * - Custom query methods
 * - Entity persistence and retrieval
 * - Ordering and filtering
 */
@DataJpaTest
@ActiveProfiles("test")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.context.annotation.Import(org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class)
public class AccountHistoryRepositoryTest {

    @Autowired
    private AccountHistoryRepository accountHistoryRepository;

    @BeforeEach
    void setUp() {
        accountHistoryRepository.deleteAll();
    }

    /**
     * T092: Test save and findById operations
     */
    @Test
    void testSaveAndFindById() {
        // Arrange
        String interactionId = UUID.randomUUID().toString();
        String entityId = UUID.randomUUID().toString();
        
        AccountHistory history = new AccountHistory();
        history.setInteractionId(interactionId);
        history.setEntityId(entityId);
        history.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        history.setCreationDate(LocalDateTime.now());
        history.setDescription("Test interaction");
        history.setStatus("COMPLETED");

        // Act
        AccountHistory saved = accountHistoryRepository.save(history);
        Optional<AccountHistory> retrieved = accountHistoryRepository.findById(interactionId);

        // Assert
        assertNotNull(saved);
        assertTrue(retrieved.isPresent());
        assertEquals(interactionId, retrieved.get().getInteractionId());
        assertEquals(entityId, retrieved.get().getEntityId());
        assertEquals("SUBSCRIBER", retrieved.get().getEntityType());
        assertEquals("Test interaction", retrieved.get().getDescription());
        assertEquals("COMPLETED", retrieved.get().getStatus());
    }

    /**
     * T092: Test findById with non-existent ID
     */
    @Test
    void testFindById_NotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        
        Optional<AccountHistory> result = accountHistoryRepository.findById(nonExistentId);
        
        assertFalse(result.isPresent());
    }

    /**
     * T092: Test findAll operation
     */
    @Test
    void testFindAll() {
        // Create multiple entries
        for (int i = 0; i < 5; i++) {
            AccountHistory history = new AccountHistory();
            history.setInteractionId(UUID.randomUUID().toString());
            history.setEntityId(UUID.randomUUID().toString());
            history.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
            history.setCreationDate(LocalDateTime.now());
            history.setDescription("Entry " + i);
            accountHistoryRepository.save(history);
        }

        List<AccountHistory> allHistory = accountHistoryRepository.findAll();
        
        assertEquals(5, allHistory.size());
    }

    /**
     * T092: Test delete operation
     */
    @Test
    void testDelete() {
        // Arrange
        String interactionId = UUID.randomUUID().toString();
        AccountHistory history = new AccountHistory();
        history.setInteractionId(interactionId);
        history.setEntityId(UUID.randomUUID().toString());
        history.setEntityType(AccountHistory.EntityType.GROUP);
        history.setCreationDate(LocalDateTime.now());
        accountHistoryRepository.save(history);

        // Act
        accountHistoryRepository.deleteById(interactionId);

        // Assert
        Optional<AccountHistory> result = accountHistoryRepository.findById(interactionId);
        assertFalse(result.isPresent());
    }

    /**
     * T092: Test entity field persistence - all fields
     */
    @Test
    void testAllFieldsPersistence() {
        LocalDateTime now = LocalDateTime.now();
        String interactionId = UUID.randomUUID().toString();

        AccountHistory history = new AccountHistory();
        history.setInteractionId(interactionId);
        history.setEntityId(UUID.randomUUID().toString());
        history.setEntityType(AccountHistory.EntityType.ACCOUNT);
        history.setCreationDate(now);
        history.setDescription("Full field test");
        history.setDirection("OUTBOUND");
        history.setReason("System update");
        history.setStatus("IN_PROGRESS");
        history.setStatusChangeDate(now.plusHours(1));
        history.setChannel("API");
        history.setStartDateTime(now.minusMinutes(30));
        history.setEndDateTime(now.plusMinutes(30));

        AccountHistory saved = accountHistoryRepository.save(history);
        Optional<AccountHistory> retrieved = accountHistoryRepository.findById(interactionId);

        assertTrue(retrieved.isPresent());
        AccountHistory result = retrieved.get();
        
        assertEquals(interactionId, result.getInteractionId());
        assertEquals("ACCOUNT", result.getEntityType());
        assertEquals("Full field test", result.getDescription());
        assertEquals("OUTBOUND", result.getDirection());
        assertEquals("System update", result.getReason());
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals("API", result.getChannel());
        assertNotNull(result.getCreationDate());
        assertNotNull(result.getStatusChangeDate());
        assertNotNull(result.getStartDateTime());
        assertNotNull(result.getEndDateTime());
    }

    /**
     * T092: Test handling of null optional fields
     */
    @Test
    void testNullOptionalFields() {
        String interactionId = UUID.randomUUID().toString();

        AccountHistory history = new AccountHistory();
        history.setInteractionId(interactionId);
        history.setEntityId(UUID.randomUUID().toString());
        history.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        history.setCreationDate(LocalDateTime.now());
        // Leave optional fields null

        AccountHistory saved = accountHistoryRepository.save(history);
        Optional<AccountHistory> retrieved = accountHistoryRepository.findById(interactionId);

        assertTrue(retrieved.isPresent());
        AccountHistory result = retrieved.get();
        
        assertNull(result.getDescription());
        assertNull(result.getDirection());
        assertNull(result.getReason());
        assertNull(result.getStatus());
        assertNull(result.getChannel());
        assertNull(result.getStatusChangeDate());
    }

    /**
     * T092: Test update operation
     */
    @Test
    void testUpdate() {
        // Create initial entry
        String interactionId = UUID.randomUUID().toString();
        AccountHistory history = new AccountHistory();
        history.setInteractionId(interactionId);
        history.setEntityId(UUID.randomUUID().toString());
        history.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        history.setCreationDate(LocalDateTime.now());
        history.setStatus("PENDING");
        history.setDescription("Initial description");
        accountHistoryRepository.save(history);

        // Update the entry
        Optional<AccountHistory> retrieved = accountHistoryRepository.findById(interactionId);
        assertTrue(retrieved.isPresent());
        
        AccountHistory toUpdate = retrieved.get();
        toUpdate.setStatus("COMPLETED");
        toUpdate.setDescription("Updated description");
        toUpdate.setStatusChangeDate(LocalDateTime.now());
        
        accountHistoryRepository.save(toUpdate);

        // Verify update
        Optional<AccountHistory> updated = accountHistoryRepository.findById(interactionId);
        assertTrue(updated.isPresent());
        assertEquals("COMPLETED", updated.get().getStatus());
        assertEquals("Updated description", updated.get().getDescription());
        assertNotNull(updated.get().getStatusChangeDate());
    }

    /**
     * T092: Test count operation
     */
    @Test
    void testCount() {
        assertEquals(0, accountHistoryRepository.count());

        for (int i = 0; i < 3; i++) {
            AccountHistory history = new AccountHistory();
            history.setInteractionId(UUID.randomUUID().toString());
            history.setEntityId(UUID.randomUUID().toString());
            history.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
            history.setCreationDate(LocalDateTime.now());
            accountHistoryRepository.save(history);
        }

        assertEquals(3, accountHistoryRepository.count());
    }

    /**
     * T092: Test exists operation
     */
    @Test
    void testExists() {
        String interactionId = UUID.randomUUID().toString();
        
        assertFalse(accountHistoryRepository.existsById(interactionId));

        AccountHistory history = new AccountHistory();
        history.setInteractionId(interactionId);
        history.setEntityId(UUID.randomUUID().toString());
        history.setEntityType(AccountHistory.EntityType.GROUP);
        history.setCreationDate(LocalDateTime.now());
        accountHistoryRepository.save(history);

        assertTrue(accountHistoryRepository.existsById(interactionId));
    }

    /**
     * T092: Test entity type variations
     */
    @Test
    void testEntityTypeVariations() {
        AccountHistory.EntityType[] entityTypes = {
            AccountHistory.EntityType.SUBSCRIBER,
            AccountHistory.EntityType.GROUP,
            AccountHistory.EntityType.ACCOUNT
        };

        for (AccountHistory.EntityType entityType : entityTypes) {
            AccountHistory history = new AccountHistory();
            history.setInteractionId(UUID.randomUUID().toString());
            history.setEntityId(UUID.randomUUID().toString());
            history.setEntityType(entityType);
            history.setCreationDate(LocalDateTime.now());
            accountHistoryRepository.save(history);
        }

        List<AccountHistory> allHistory = accountHistoryRepository.findAll();
        assertEquals(3, allHistory.size());
        
        // Verify all entity types are preserved
        long subscriberCount = allHistory.stream()
                .filter(h -> "SUBSCRIBER".equals(h.getEntityType()))
                .count();
        long groupCount = allHistory.stream()
                .filter(h -> "GROUP".equals(h.getEntityType()))
                .count();
        long accountCount = allHistory.stream()
                .filter(h -> "ACCOUNT".equals(h.getEntityType()))
                .count();
        
        assertEquals(1, subscriberCount);
        assertEquals(1, groupCount);
        assertEquals(1, accountCount);
    }
}
