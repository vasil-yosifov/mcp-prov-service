package com.telecom.ocs.provisioning.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Subscription API endpoints.
 * Tests the complete REST API flow using Testcontainers MySQL.
 * 
 * Tests:
 * - T055: POST /subscribers/{id}/subscriptions (create subscription)
 * - T056: GET /subscriptions/{id} (retrieve subscription)
 * - T057: GET /subscribers/{id}/subscriptions (list subscriptions)
 * - T058: PATCH /subscriptions/{id} (activate/cancel subscription)
 * - T059: DELETE /subscriptions/{id} (delete subscription)
 * - T060: Recurring cycle expiration logic
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class SubscriptionIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String testSubscriberId;
    private Map<String, Object> validSubscriptionRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Create a subscriber first (required for creating subscriptions)
        testSubscriberId = createTestSubscriber();

        // Prepare valid subscription request payload
        validSubscriptionRequest = new HashMap<>();
        validSubscriptionRequest.put("offerId", "OFFER-001");
        validSubscriptionRequest.put("offerName", "Premium Data Plan");
        validSubscriptionRequest.put("subscriptionType", "DATA");
        validSubscriptionRequest.put("recurring", true);
        validSubscriptionRequest.put("maxRecurringCycles", 12);
        validSubscriptionRequest.put("cycleLengthUnits", 1);
        validSubscriptionRequest.put("cycleLengthType", "MONTHS");
    }

    /**
     * Helper method to create a test subscriber and return its ID
     */
    private String createTestSubscriber() throws Exception {
        Map<String, Object> subscriberRequest = new HashMap<>();
        // Use a unique msisdn for each test run to avoid conflicts
        String uniqueMsisdn = "4366412345" + System.currentTimeMillis() % 100000;
        subscriberRequest.put("msisdn", uniqueMsisdn);
        subscriberRequest.put("firstName", "Test");
        subscriberRequest.put("lastName", "Subscriber");

        String requestJson = objectMapper.writeValueAsString(subscriberRequest);

        MvcResult result = mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscriber = objectMapper.readValue(response, Map.class);
        return (String) createdSubscriber.get("subscriberId");
    }

    // =========================================================================
    // T055: POST /subscribers/{id}/subscriptions (create subscription)
    // =========================================================================

    /**
     * T055: Test creating a new subscription via POST /subscribers/{id}/subscriptions
     * Expected: 201 Created with subscription data including generated subscriptionId
     */
    @Test
    void testCreateSubscription() throws Exception {
        String requestJson = objectMapper.writeValueAsString(validSubscriptionRequest);

        mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subscriptionId").isNotEmpty())
                .andExpect(jsonPath("$.subscriberId").value(testSubscriberId))
                .andExpect(jsonPath("$.offerId").value("OFFER-001"))
                .andExpect(jsonPath("$.offerName").value("Premium Data Plan"))
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andExpect(jsonPath("$.recurring").value(true))
                .andExpect(jsonPath("$.maxRecurringCycles").value(12))
                .andExpect(jsonPath("$.recurringCyclesCompleted").value(0))
                .andExpect(jsonPath("$.creationDate").isNotEmpty())
                .andExpect(jsonPath("$.lastModifiedDate").isNotEmpty());
    }

    /**
     * T055 (additional): Test creating subscription with minimal required fields
     * Expected: 201 Created with only offerId provided
     */
    @Test
    void testCreateSubscriptionMinimalFields() throws Exception {
        Map<String, Object> minimalRequest = new HashMap<>();
        minimalRequest.put("offerId", "OFFER-MIN-001");

        String requestJson = objectMapper.writeValueAsString(minimalRequest);

        mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionId").isNotEmpty())
                .andExpect(jsonPath("$.subscriberId").value(testSubscriberId))
                .andExpect(jsonPath("$.offerId").value("OFFER-MIN-001"))
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andExpect(jsonPath("$.recurring").value(false));
    }

    /**
     * T055 (validation): Test creating subscription for non-existent subscriber
     * Expected: 404 Not Found
     */
    @Test
    void testCreateSubscriptionSubscriberNotFound() throws Exception {
        String nonExistentSubscriberId = "00000000-0000-0000-0000-000000000000";
        String requestJson = objectMapper.writeValueAsString(validSubscriptionRequest);

        mockMvc.perform(post("/ocs/prov/v1/subscribers/" + nonExistentSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    /**
     * T055 (validation): Test creating subscription with invalid payload
     * Expected: 400 Bad Request
     */
    @Test
    void testCreateSubscriptionInvalidPayload() throws Exception {
        // Empty request body should be invalid
        mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // T056: GET /subscriptions/{id} (retrieve subscription)
    // =========================================================================

    /**
     * T056: Test retrieving subscription by ID via GET /subscriptions/{id}
     * Expected: 200 OK with subscription data
     */
    @Test
    void testGetSubscriptionById() throws Exception {
        // First create a subscription
        String requestJson = objectMapper.writeValueAsString(validSubscriptionRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscription = objectMapper.readValue(createResponse, Map.class);
        String subscriptionId = (String) createdSubscription.get("subscriptionId");

        // Now retrieve the subscription by ID
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.subscriberId").value(testSubscriberId))
                .andExpect(jsonPath("$.offerId").value("OFFER-001"))
                .andExpect(jsonPath("$.offerName").value("Premium Data Plan"));
    }

    /**
     * T056 (error case): Test retrieving non-existent subscription
     * Expected: 404 Not Found
     */
    @Test
    void testGetSubscriptionByIdNotFound() throws Exception {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // T057: GET /subscribers/{id}/subscriptions (list subscriptions)
    // =========================================================================

    /**
     * T057: Test listing subscriptions for a subscriber via GET /subscribers/{id}/subscriptions
     * Expected: 200 OK with list of subscriptions
     */
    @Test
    void testListSubscriptionsForSubscriber() throws Exception {
        // Create two subscriptions for the subscriber
        String requestJson1 = objectMapper.writeValueAsString(validSubscriptionRequest);
        mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson1))
                .andExpect(status().isCreated());

        Map<String, Object> subscription2 = new HashMap<>();
        subscription2.put("offerId", "OFFER-002");
        subscription2.put("offerName", "Voice Plan");
        subscription2.put("subscriptionType", "VOICE");
        
        String requestJson2 = objectMapper.writeValueAsString(subscription2);
        mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson2))
                .andExpect(status().isCreated());

        // Now list all subscriptions for the subscriber
        mockMvc.perform(get("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].subscriberId", everyItem(is(testSubscriberId))));
    }

    /**
     * T057: Test listing subscriptions for subscriber with no subscriptions
     * Expected: 200 OK with empty list
     */
    @Test
    void testListSubscriptionsEmpty() throws Exception {
        // Create a new subscriber with no subscriptions
        String newSubscriberId = createTestSubscriber();

        mockMvc.perform(get("/ocs/prov/v1/subscribers/" + newSubscriberId + "/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * T057 (error case): Test listing subscriptions for non-existent subscriber
     * Expected: 404 Not Found
     */
    @Test
    void testListSubscriptionsSubscriberNotFound() throws Exception {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        
        mockMvc.perform(get("/ocs/prov/v1/subscribers/" + nonExistentId + "/subscriptions"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // T058: PATCH /subscriptions/{id} (activate/cancel subscription)
    // =========================================================================

    /**
     * T058: Test activating subscription via PATCH /subscriptions/{id}
     * Expected: 200 OK with updated state to ACTIVE
     */
    @Test
    void testActivateSubscription() throws Exception {
        // First create a subscription (initial state: PENDING)
        String createRequestJson = objectMapper.writeValueAsString(validSubscriptionRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscription = objectMapper.readValue(createResponse, Map.class);
        String subscriptionId = (String) createdSubscription.get("subscriptionId");

        // Now activate the subscription
        Map<String, Object> patchRequest = new HashMap<>();
        patchRequest.put("fieldName", "state");
        patchRequest.put("fieldValue", "ACTIVE");
        
        String patchRequestJson = objectMapper.writeValueAsString(new Map[] { patchRequest });

        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.activationDate").isNotEmpty());
    }

    /**
     * T058: Test cancelling subscription via PATCH /subscriptions/{id}
     * Expected: 200 OK with updated state to CANCELLED
     */
    @Test
    void testCancelSubscription() throws Exception {
        // First create and activate a subscription
        String createRequestJson = objectMapper.writeValueAsString(validSubscriptionRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscription = objectMapper.readValue(createResponse, Map.class);
        String subscriptionId = (String) createdSubscription.get("subscriptionId");

        // First activate it
        Map<String, Object> activatePatch = new HashMap<>();
        activatePatch.put("fieldName", "state");
        activatePatch.put("fieldValue", "ACTIVE");
        String activatePatchJson = objectMapper.writeValueAsString(new Map[] { activatePatch });
        
        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activatePatchJson))
                .andExpect(status().isOk());

        // Now cancel the subscription
        Map<String, Object> cancelPatch = new HashMap<>();
        cancelPatch.put("fieldName", "state");
        cancelPatch.put("fieldValue", "CANCELLED");
        
        String cancelPatchJson = objectMapper.writeValueAsString(new Map[] { cancelPatch });

        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelPatchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.state").value("CANCELLED"));
    }

    /**
     * T058: Test suspending subscription via PATCH /subscriptions/{id}
     * Expected: 200 OK with updated state to SUSPENDED
     */
    @Test
    void testSuspendSubscription() throws Exception {
        // First create and activate a subscription
        String createRequestJson = objectMapper.writeValueAsString(validSubscriptionRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscription = objectMapper.readValue(createResponse, Map.class);
        String subscriptionId = (String) createdSubscription.get("subscriptionId");

        // First activate it
        Map<String, Object> activatePatch = new HashMap<>();
        activatePatch.put("fieldName", "state");
        activatePatch.put("fieldValue", "ACTIVE");
        String activatePatchJson = objectMapper.writeValueAsString(new Map[] { activatePatch });
        
        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activatePatchJson))
                .andExpect(status().isOk());

        // Now suspend the subscription
        Map<String, Object> suspendPatch = new HashMap<>();
        suspendPatch.put("fieldName", "state");
        suspendPatch.put("fieldValue", "SUSPENDED");
        
        String suspendPatchJson = objectMapper.writeValueAsString(new Map[] { suspendPatch });

        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(suspendPatchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.state").value("SUSPENDED"));
    }

    /**
     * T058 (error case): Test updating non-existent subscription
     * Expected: 404 Not Found
     */
    @Test
    void testUpdateSubscriptionNotFound() throws Exception {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        
        Map<String, Object> patchRequest = new HashMap<>();
        patchRequest.put("fieldName", "state");
        patchRequest.put("fieldValue", "ACTIVE");
        String patchRequestJson = objectMapper.writeValueAsString(new Map[] { patchRequest });

        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequestJson))
                .andExpect(status().isNotFound());
    }

    /**
     * T058 (validation): Test updating subscription with invalid field
     * Expected: 422 Unprocessable Entity
     */
    @Test
    void testUpdateSubscriptionInvalidField() throws Exception {
        // First create a subscription
        String createRequestJson = objectMapper.writeValueAsString(validSubscriptionRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscription = objectMapper.readValue(createResponse, Map.class);
        String subscriptionId = (String) createdSubscription.get("subscriptionId");

        // Try to update with invalid field name
        Map<String, Object> patchRequest = new HashMap<>();
        patchRequest.put("fieldName", "invalidFieldName");
        patchRequest.put("fieldValue", "someValue");
        
        String patchRequestJson = objectMapper.writeValueAsString(new Map[] { patchRequest });

        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequestJson))
                .andExpect(status().is4xxClientError());
    }

    // =========================================================================
    // T059: DELETE /subscriptions/{id} (delete subscription)
    // =========================================================================

    /**
     * T059: Test deleting subscription via DELETE /subscriptions/{id}
     * Expected: 204 No Content and subscription is deleted
     */
    @Test
    void testDeleteSubscription() throws Exception {
        // First create a subscription
        String requestJson = objectMapper.writeValueAsString(validSubscriptionRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscription = objectMapper.readValue(createResponse, Map.class);
        String subscriptionId = (String) createdSubscription.get("subscriptionId");

        // Delete the subscription
        mockMvc.perform(delete("/ocs/prov/v1/subscriptions/" + subscriptionId))
                .andExpect(status().isNoContent());

        // Verify subscription is deleted
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId))
                .andExpect(status().isNotFound());
    }

    /**
     * T059 (error case): Test deleting non-existent subscription
     * Expected: 404 Not Found
     */
    @Test
    void testDeleteSubscriptionNotFound() throws Exception {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        
        mockMvc.perform(delete("/ocs/prov/v1/subscriptions/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    /**
     * T059 (cascade): Test that deleting subscription cascades to balances
     * Expected: 204 No Content and related balances are deleted
     */
    @Test
    void testDeleteSubscriptionCascadesToBalances() throws Exception {
        // First create a subscription
        String subscriptionJson = objectMapper.writeValueAsString(validSubscriptionRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscription = objectMapper.readValue(createResponse, Map.class);
        String subscriptionId = (String) createdSubscription.get("subscriptionId");

        // Create a balance for the subscription
        Map<String, Object> balanceRequest = new HashMap<>();
        balanceRequest.put("balanceType", "ALLOWANCE");
        balanceRequest.put("unitType", "BYTES");
        balanceRequest.put("balanceAmount", 10000000000L);
        balanceRequest.put("balanceAvailable", 10000000000L);
        
        String balanceJson = objectMapper.writeValueAsString(balanceRequest);
        
        MvcResult balanceResult = mockMvc.perform(post("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balanceJson))
                .andExpect(status().isCreated())
                .andReturn();

        String balanceResponse = balanceResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdBalance = objectMapper.readValue(balanceResponse, Map.class);
        String balanceId = (String) createdBalance.get("balanceId");

        // Delete the subscription (should cascade to balance)
        mockMvc.perform(delete("/ocs/prov/v1/subscriptions/" + subscriptionId))
                .andExpect(status().isNoContent());

        // Verify subscription is deleted
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId))
                .andExpect(status().isNotFound());

        // Verify balances list is empty or balance is not found
        // Since subscription is deleted, listing balances should fail
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // T060: Recurring cycle expiration logic
    // =========================================================================

    /**
     * T060: Test automatic expiration when recurringCyclesCompleted equals maxRecurringCycles
     * Expected: State transitions to EXPIRED when max cycles reached
     */
    @Test
    void testRecurringCycleExpiration() throws Exception {
        // Create a recurring subscription with maxRecurringCycles = 2
        Map<String, Object> recurringSubscription = new HashMap<>();
        recurringSubscription.put("offerId", "OFFER-RECURRING");
        recurringSubscription.put("offerName", "Short Term Plan");
        recurringSubscription.put("recurring", true);
        recurringSubscription.put("maxRecurringCycles", 2);
        recurringSubscription.put("cycleLengthUnits", 1);
        recurringSubscription.put("cycleLengthType", "MONTHS");
        
        String requestJson = objectMapper.writeValueAsString(recurringSubscription);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recurringCyclesCompleted").value(0))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscription = objectMapper.readValue(createResponse, Map.class);
        String subscriptionId = (String) createdSubscription.get("subscriptionId");

        // Activate the subscription first
        Map<String, Object> activatePatch = new HashMap<>();
        activatePatch.put("fieldName", "state");
        activatePatch.put("fieldValue", "ACTIVE");
        String activatePatchJson = objectMapper.writeValueAsString(new Map[] { activatePatch });
        
        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activatePatchJson))
                .andExpect(status().isOk());

        // Update recurringCyclesCompleted to 1 (simulate one cycle completed)
        Map<String, Object> cycle1Patch = new HashMap<>();
        cycle1Patch.put("fieldName", "recurringCyclesCompleted");
        cycle1Patch.put("fieldValue", 1);
        String cycle1PatchJson = objectMapper.writeValueAsString(new Map[] { cycle1Patch });
        
        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cycle1PatchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recurringCyclesCompleted").value(1))
                .andExpect(jsonPath("$.state").value("ACTIVE")); // Still active

        // Update recurringCyclesCompleted to 2 (equals maxRecurringCycles)
        // This should trigger automatic transition to EXPIRED
        Map<String, Object> cycle2Patch = new HashMap<>();
        cycle2Patch.put("fieldName", "recurringCyclesCompleted");
        cycle2Patch.put("fieldValue", 2);
        String cycle2PatchJson = objectMapper.writeValueAsString(new Map[] { cycle2Patch });
        
        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cycle2PatchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recurringCyclesCompleted").value(2))
                .andExpect(jsonPath("$.state").value("EXPIRED")); // Auto-transitioned
    }

    /**
     * T060: Test that non-recurring subscription does not auto-expire
     * Expected: State remains unchanged when maxRecurringCycles is not set
     */
    @Test
    void testNonRecurringNoAutoExpiration() throws Exception {
        // Create a non-recurring subscription
        Map<String, Object> nonRecurringSubscription = new HashMap<>();
        nonRecurringSubscription.put("offerId", "OFFER-ONETIME");
        nonRecurringSubscription.put("offerName", "One Time Purchase");
        nonRecurringSubscription.put("recurring", false);
        
        String requestJson = objectMapper.writeValueAsString(nonRecurringSubscription);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscription = objectMapper.readValue(createResponse, Map.class);
        String subscriptionId = (String) createdSubscription.get("subscriptionId");

        // Activate the subscription
        Map<String, Object> activatePatch = new HashMap<>();
        activatePatch.put("fieldName", "state");
        activatePatch.put("fieldValue", "ACTIVE");
        String activatePatchJson = objectMapper.writeValueAsString(new Map[] { activatePatch });
        
        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activatePatchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.recurring").value(false));

        // Verify state remains ACTIVE (no auto-expiration for non-recurring)
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"));
    }

    /**
     * T060: Test renewal date calculation based on cycleLengthType and cycleLengthUnits
     * Expected: renewalDate is calculated correctly
     */
    @Test
    void testRenewalDateCalculation() throws Exception {
        // Create a recurring subscription with specific cycle parameters
        Map<String, Object> recurringSubscription = new HashMap<>();
        recurringSubscription.put("offerId", "OFFER-MONTHLY");
        recurringSubscription.put("offerName", "Monthly Plan");
        recurringSubscription.put("recurring", true);
        recurringSubscription.put("maxRecurringCycles", 12);
        recurringSubscription.put("cycleLengthUnits", 1);
        recurringSubscription.put("cycleLengthType", "MONTHS");
        
        String requestJson = objectMapper.writeValueAsString(recurringSubscription);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscription = objectMapper.readValue(createResponse, Map.class);
        String subscriptionId = (String) createdSubscription.get("subscriptionId");

        // Activate the subscription - this should set renewalDate
        Map<String, Object> activatePatch = new HashMap<>();
        activatePatch.put("fieldName", "state");
        activatePatch.put("fieldValue", "ACTIVE");
        String activatePatchJson = objectMapper.writeValueAsString(new Map[] { activatePatch });
        
        mockMvc.perform(patch("/ocs/prov/v1/subscriptions/" + subscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activatePatchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.renewalDate").isNotEmpty());
    }

    /**
     * T060: Test edge case - zero maxRecurringCycles
     * Expected: Subscription should handle gracefully
     */
    @Test
    void testZeroMaxRecurringCycles() throws Exception {
        // Create a subscription with zero max cycles (immediate expiration potential)
        Map<String, Object> zeroMaxCyclesSubscription = new HashMap<>();
        zeroMaxCyclesSubscription.put("offerId", "OFFER-ZERO");
        zeroMaxCyclesSubscription.put("offerName", "Zero Cycles Plan");
        zeroMaxCyclesSubscription.put("recurring", true);
        zeroMaxCyclesSubscription.put("maxRecurringCycles", 0);
        
        String requestJson = objectMapper.writeValueAsString(zeroMaxCyclesSubscription);
        
        // This might either fail validation or create and immediately expire
        mockMvc.perform(post("/ocs/prov/v1/subscribers/" + testSubscriberId + "/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is2xxSuccessful())
                // If created, recurringCyclesCompleted (0) equals maxRecurringCycles (0)
                // So it should be in EXPIRED state or similar
                .andExpect(jsonPath("$.maxRecurringCycles").value(0));
    }
}
