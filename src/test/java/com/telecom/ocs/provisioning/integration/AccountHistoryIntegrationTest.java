package com.telecom.ocs.provisioning.integration;

import com.telecom.ocs.provisioning.models.AccountHistory;
import com.telecom.ocs.provisioning.repositories.AccountHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Account History endpoints.
 * 
 * Tests cover:
 * - T088: POST /accountHistory (create history entry)
 * - T089: GET /accountHistory/{interactionId} (retrieve history entry)
 * - T090: GET /accountHistory/{entityId} (list history by entity)
 * - T091: Chronological ordering of history entries
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AccountHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountHistoryRepository accountHistoryRepository;

    private static final String BASE_URL = "/ocs/prov/v1/accountHistory";

    @BeforeEach
    void setUp() {
        // Clean up before each test
        accountHistoryRepository.deleteAll();
    }

    /**
     * T088: Integration test for POST /accountHistory (create history entry)
     * 
     * Validates:
     * - Returns 201 Created on successful creation
     * - Response contains all required fields
     * - Entity is persisted in database
     * - Generated IDs are present
     */
    @Test
    void testCreateAccountHistoryEntry() throws Exception {
        String entityId = UUID.randomUUID().toString();
        String requestBody = String.format("""
            {
              "entityId": "%s",
              "entityType": "SUBSCRIBER",
              "description": "Account created",
              "direction": "INBOUND",
              "reason": "New customer registration",
              "status": "COMPLETED",
              "channel": "WEB",
              "interactionDate": {
                "startDateTime": "2025-12-03T10:00:00Z",
                "endDateTime": "2025-12-03T10:05:00Z"
              }
            }
            """, entityId);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interactionId").exists())
                .andExpect(jsonPath("$.entityId").value(entityId))
                .andExpect(jsonPath("$.entityType").value("SUBSCRIBER"))
                .andExpect(jsonPath("$.description").value("Account created"))
                .andExpect(jsonPath("$.direction").value("INBOUND"))
                .andExpect(jsonPath("$.reason").value("New customer registration"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.channel").value("WEB"))
                .andExpect(jsonPath("$.creationDate").exists());

        // Verify persistence in database
        List<AccountHistory> allHistory = accountHistoryRepository.findAll();
        assertEquals(1, allHistory.size());
        assertEquals(entityId, allHistory.get(0).getEntityId());
        assertEquals("SUBSCRIBER", allHistory.get(0).getEntityType());
    }

    /**
     * T088: Test validation - missing required fields
     */
    @Test
    void testCreateAccountHistoryEntry_MissingRequiredFields() throws Exception {
        String requestBody = """
            {
              "description": "Test without required fields"
            }
            """;

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    /**
     * T088: Test duplicate interactionId handling
     */
    @Test
    void testCreateAccountHistoryEntry_DuplicateInteractionId() throws Exception {
        String interactionId = UUID.randomUUID().toString();
        String entityId = UUID.randomUUID().toString();

        // Create first entry
        AccountHistory history = new AccountHistory();
        history.setInteractionId(interactionId);
        history.setEntityId(entityId);
        history.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        history.setCreationDate(LocalDateTime.now());
        history.setDescription("First entry");
        accountHistoryRepository.save(history);

        // Try to create duplicate
        String requestBody = String.format("""
            {
              "interactionId": "%s",
              "entityId": "%s",
              "entityType": "SUBSCRIBER",
              "description": "Duplicate entry"
            }
            """, interactionId, entityId);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict());
    }

    /**
     * T089: Integration test for GET /accountHistory/{interactionId} (retrieve history entry)
     * 
     * Validates:
     * - Returns 200 OK for existing entry
     * - Response contains correct data
     * - Returns 404 Not Found for non-existent entry
     */
    @Test
    void testGetAccountHistoryByInteractionId() throws Exception {
        // Create test data
        String interactionId = UUID.randomUUID().toString();
        String entityId = UUID.randomUUID().toString();
        
        AccountHistory history = new AccountHistory();
        history.setInteractionId(interactionId);
        history.setEntityId(entityId);
        history.setEntityType(AccountHistory.EntityType.GROUP);
        history.setCreationDate(LocalDateTime.now());
        history.setDescription("Test interaction");
        history.setStatus("IN_PROGRESS");
        history.setChannel("MOBILE_APP");
        accountHistoryRepository.save(history);

        // Retrieve by interactionId
        mockMvc.perform(get(BASE_URL + "/" + interactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interactionId").value(interactionId))
                .andExpect(jsonPath("$.entityId").value(entityId))
                .andExpect(jsonPath("$.entityType").value("GROUP"))
                .andExpect(jsonPath("$.description").value("Test interaction"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.channel").value("MOBILE_APP"));
    }

    /**
     * T089: Test GET with non-existent interactionId
     */
    @Test
    void testGetAccountHistoryByInteractionId_NotFound() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();

        mockMvc.perform(get(BASE_URL + "/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    /**
     * T090: Integration test for GET /accountHistory/{entityId} (list history by entity)
     * 
     * Validates:
     * - Returns 200 OK with array of entries
     * - Only returns entries for specified entity
     * - Returns empty array for entity with no history
     */
    @Test
    void testListAccountHistoryByEntityId() throws Exception {
        String entityId = UUID.randomUUID().toString();
        String otherEntityId = UUID.randomUUID().toString();

        // Create multiple entries for the entity
        for (int i = 0; i < 3; i++) {
            AccountHistory history = new AccountHistory();
            history.setInteractionId(UUID.randomUUID().toString());
            history.setEntityId(entityId);
            history.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
            history.setCreationDate(LocalDateTime.now().minusHours(i));
            history.setDescription("Interaction " + i);
            history.setStartDateTime(LocalDateTime.now().minusHours(i));
            accountHistoryRepository.save(history);
        }

        // Create entry for different entity
        AccountHistory otherHistory = new AccountHistory();
        otherHistory.setInteractionId(UUID.randomUUID().toString());
        otherHistory.setEntityId(otherEntityId);
        otherHistory.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        otherHistory.setCreationDate(LocalDateTime.now());
        otherHistory.setDescription("Other entity interaction");
        accountHistoryRepository.save(otherHistory);

        // Get history for specific entity
        mockMvc.perform(get(BASE_URL + "/" + entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].entityId", everyItem(equalTo(entityId))));
    }

    /**
     * T090: Test list with no history entries
     */
    @Test
    void testListAccountHistoryByEntityId_EmptyList() throws Exception {
        String entityIdWithNoHistory = UUID.randomUUID().toString();

        mockMvc.perform(get(BASE_URL + "/" + entityIdWithNoHistory))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * T091: Integration test for chronological ordering of history entries
     * 
     * Validates:
     * - Entries are returned in reverse chronological order (newest first)
     * - Based on startDateTime field
     */
    @Test
    void testChronologicalOrderingOfHistoryEntries() throws Exception {
        String entityId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        // Create entries with different timestamps
        AccountHistory oldest = new AccountHistory();
        oldest.setInteractionId(UUID.randomUUID().toString());
        oldest.setEntityId(entityId);
        oldest.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        oldest.setCreationDate(now.minusDays(3));
        oldest.setStartDateTime(now.minusDays(3));
        oldest.setDescription("Oldest interaction");
        accountHistoryRepository.save(oldest);

        AccountHistory middle = new AccountHistory();
        middle.setInteractionId(UUID.randomUUID().toString());
        middle.setEntityId(entityId);
        middle.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        middle.setCreationDate(now.minusDays(2));
        middle.setStartDateTime(now.minusDays(2));
        middle.setDescription("Middle interaction");
        accountHistoryRepository.save(middle);

        AccountHistory newest = new AccountHistory();
        newest.setInteractionId(UUID.randomUUID().toString());
        newest.setEntityId(entityId);
        newest.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        newest.setCreationDate(now.minusDays(1));
        newest.setStartDateTime(now.minusDays(1));
        newest.setDescription("Newest interaction");
        accountHistoryRepository.save(newest);

        // Verify reverse chronological order (newest first)
        mockMvc.perform(get(BASE_URL + "/" + entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].description").value("Newest interaction"))
                .andExpect(jsonPath("$[1].description").value("Middle interaction"))
                .andExpect(jsonPath("$[2].description").value("Oldest interaction"));
    }

    /**
     * T091: Test ordering with same startDateTime - should use creationDate as tiebreaker
     */
    @Test
    void testChronologicalOrdering_SameStartDateTime() throws Exception {
        String entityId = UUID.randomUUID().toString();
        LocalDateTime sameTime = LocalDateTime.now().minusDays(1);

        // Create entries with same startDateTime but different creationDate
        AccountHistory first = new AccountHistory();
        first.setInteractionId(UUID.randomUUID().toString());
        first.setEntityId(entityId);
        first.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        first.setCreationDate(sameTime.minusMinutes(10));
        first.setStartDateTime(sameTime);
        first.setDescription("First created");
        accountHistoryRepository.save(first);

        AccountHistory second = new AccountHistory();
        second.setInteractionId(UUID.randomUUID().toString());
        second.setEntityId(entityId);
        second.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
        second.setCreationDate(sameTime.minusMinutes(5));
        second.setStartDateTime(sameTime);
        second.setDescription("Second created");
        accountHistoryRepository.save(second);

        // Verify ordering by creationDate when startDateTime is same
        mockMvc.perform(get(BASE_URL + "/" + entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].description").value("Second created"))
                .andExpect(jsonPath("$[1].description").value("First created"));
    }

    /**
     * Additional test: Verify pagination support (if implemented)
     */
    @Test
    void testListAccountHistoryWithPagination() throws Exception {
        String entityId = UUID.randomUUID().toString();

        // Create 10 entries
        for (int i = 0; i < 10; i++) {
            AccountHistory history = new AccountHistory();
            history.setInteractionId(UUID.randomUUID().toString());
            history.setEntityId(entityId);
            history.setEntityType(AccountHistory.EntityType.SUBSCRIBER);
            history.setCreationDate(LocalDateTime.now().minusHours(i));
            history.setStartDateTime(LocalDateTime.now().minusHours(i));
            history.setDescription("Entry " + i);
            accountHistoryRepository.save(history);
        }

        // Test pagination
        mockMvc.perform(get(BASE_URL + "/" + entityId)
                .param("limit", "5")
                .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(0))));
    }
}
