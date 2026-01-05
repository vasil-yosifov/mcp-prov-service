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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Usage API endpoints.
 * Tests the complete REST API flow using Testcontainers MySQL.
 * 
 * Tests:
 * - T170: POST /usage (create usage record)
 * - T171: Voice usage recording with ALLOWANCE balance deduction
 * - T172: Data usage recording with ALLOWANCE balance deduction
 * - T173: SMS/MMS usage recording
 * - T174: Duplicate usageId validation (409 Conflict)
 * - T175: Invalid chargedPartyId (404 Not Found)
 * 
 * Balance update logic (FR-096, FR-097, FR-098):
 * - ALLOWANCE: volumeUsage deducted from balanceAvailable (FR-096)
 * - ALLOWANCE overflow: balanceAvailable set to 0 when volumeUsage exceeds available (FR-097)
 * - COUNTER: volumeUsage added to balanceAvailable (FR-098)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class UsageIntegrationTest {

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

    private String subscriberId;
    private String subscriptionId;
    private String voiceBalanceId;
    private String dataBalanceId;
    private String smsBalanceId;
    private String counterBalanceId;

    @BeforeEach
    void setUp() throws Exception {
        // Create subscriber
        Map<String, Object> subscriber = new HashMap<>();
        subscriber.put("msisdn", "43664" + System.currentTimeMillis());
        subscriber.put("imsi", "21401" + System.currentTimeMillis());
        subscriber.put("firstName", "Usage");
        subscriber.put("lastName", "Test");
        subscriber.put("dateOfBirth", "1990-01-15");
        subscriber.put("email", "usage.test@example.com");
        subscriber.put("contactNumber", "436641234567");
        subscriber.put("billingCycle", 1);
        subscriber.put("billingAddress", "Test Address");

        MvcResult subscriberResult = mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subscriber)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> subscriberResponse = objectMapper.readValue(
                subscriberResult.getResponse().getContentAsString(), Map.class);
        subscriberId = (String) subscriberResponse.get("subscriberId");

        // Create subscription
        Map<String, Object> subscription = new HashMap<>();
        subscription.put("subscriberId", subscriberId);
        subscription.put("offerId", "OFFER_VOICE_DATA");
        subscription.put("offerName", "Voice & Data Plan");
        subscription.put("isRecurring", true);
        subscription.put("maxRecurringCycles", 12);
        subscription.put("cycleLengthType", "months");
        subscription.put("cycleLengthUnits", 1);

        MvcResult subscriptionResult = mockMvc.perform(post("/ocs/prov/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subscription)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> subscriptionResponse = objectMapper.readValue(
                subscriptionResult.getResponse().getContentAsString(), Map.class);
        subscriptionId = (String) subscriptionResponse.get("subscriptionId");

        // Create VOICE ALLOWANCE balance (1000 seconds = ~16.6 minutes)
        Map<String, Object> voiceBalance = new HashMap<>();
        voiceBalance.put("balanceType", "ALLOWANCE");
        voiceBalance.put("unitType", "SECONDS");
        voiceBalance.put("balanceAmount", 1000);
        voiceBalance.put("balanceAvailable", 1000);
        voiceBalance.put("isRolloverAllowed", false);
        voiceBalance.put("isRecurring", true);
        voiceBalance.put("isGroupBalance", false);

        MvcResult voiceBalanceResult = mockMvc.perform(post("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voiceBalance)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> voiceBalanceResponse = objectMapper.readValue(
                voiceBalanceResult.getResponse().getContentAsString(), Map.class);
        voiceBalanceId = (String) voiceBalanceResponse.get("balanceId");

        // Create DATA ALLOWANCE balance (100 MB = 104857600 bytes)
        Map<String, Object> dataBalance = new HashMap<>();
        dataBalance.put("balanceType", "ALLOWANCE");
        dataBalance.put("unitType", "BYTES");
        dataBalance.put("balanceAmount", 104857600);
        dataBalance.put("balanceAvailable", 104857600);
        dataBalance.put("isRolloverAllowed", false);
        dataBalance.put("isRecurring", true);
        dataBalance.put("isGroupBalance", false);

        MvcResult dataBalanceResult = mockMvc.perform(post("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dataBalance)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> dataBalanceResponse = objectMapper.readValue(
                dataBalanceResult.getResponse().getContentAsString(), Map.class);
        dataBalanceId = (String) dataBalanceResponse.get("balanceId");

        // Create SMS ALLOWANCE balance (10 SMS)
        Map<String, Object> smsBalance = new HashMap<>();
        smsBalance.put("balanceType", "ALLOWANCE");
        smsBalance.put("unitType", "EVENTS");
        smsBalance.put("balanceAmount", 10);
        smsBalance.put("balanceAvailable", 10);
        smsBalance.put("isRolloverAllowed", false);
        smsBalance.put("isRecurring", true);
        smsBalance.put("isGroupBalance", false);

        MvcResult smsBalanceResult = mockMvc.perform(post("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(smsBalance)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> smsBalanceResponse = objectMapper.readValue(
                smsBalanceResult.getResponse().getContentAsString(), Map.class);
        smsBalanceId = (String) smsBalanceResponse.get("balanceId");

        // Create COUNTER balance (starts at 500)
        Map<String, Object> counterBalance = new HashMap<>();
        counterBalance.put("balanceType", "COUNTER");
        counterBalance.put("unitType", "EVENTS");
        counterBalance.put("balanceAmount", 500);
        counterBalance.put("balanceAvailable", 500);
        counterBalance.put("isRolloverAllowed", false);
        counterBalance.put("isRecurring", false);
        counterBalance.put("isGroupBalance", false);

        MvcResult counterBalanceResult = mockMvc.perform(post("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(counterBalance)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> counterBalanceResponse = objectMapper.readValue(
                counterBalanceResult.getResponse().getContentAsString(), Map.class);
        counterBalanceId = (String) counterBalanceResponse.get("balanceId");
    }

    /**
     * T170: Test creating a new usage record via POST /usage
     * Expected: 201 Created with usage data including generated usageId
     */
    @Test
    void testCreateUsageRecord() throws Exception {
        Map<String, Object> usageRequest = createUsageRequest(
                voiceBalanceId, "VOICE", "EVENT", BigDecimal.valueOf(60));

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usageRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.usageId").isNotEmpty())
                .andExpect(jsonPath("$.chargedPartyId").value(subscriberId))
                .andExpect(jsonPath("$.usageType").value("VOICE"))
                .andExpect(jsonPath("$.recordType").value("EVENT"))
                .andExpect(jsonPath("$.volumeUsage").value(60))
                .andExpect(jsonPath("$.impactedBalanceId").value(voiceBalanceId))
                .andExpect(jsonPath("$.balanceValueBefore").value(1000))
                .andExpect(jsonPath("$.balanceValueAfter").value(940))
                .andExpect(jsonPath("$.usageTimestamp").isNotEmpty());

        // Verify balance was deducted (FR-096: ALLOWANCE deduction)
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances/" + voiceBalanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAvailable").value(940));
    }

    /**
     * T171: Test voice usage recording with ALLOWANCE balance deduction
     * Expected: Usage record created, balance deducted correctly (FR-096)
     */
    @Test
    void testVoiceUsageWithAllowanceDeduction() throws Exception {
        // Use 300 seconds (5 minutes) from 1000 seconds balance
        Map<String, Object> usageRequest = createUsageRequest(
                voiceBalanceId, "VOICE", "STOP", BigDecimal.valueOf(300));
        usageRequest.put("recordOpeningTime", OffsetDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        usageRequest.put("recordClosingTime", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        usageRequest.put("durationSeconds", 300);
        usageRequest.put("aParty", "43664123456789");
        usageRequest.put("bParty", "43664987654321");

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usageType").value("VOICE"))
                .andExpect(jsonPath("$.recordType").value("STOP"))
                .andExpect(jsonPath("$.volumeUsage").value(300))
                .andExpect(jsonPath("$.balanceValueBefore").value(1000))
                .andExpect(jsonPath("$.balanceValueAfter").value(700))
                .andExpect(jsonPath("$.durationSeconds").value(300));

        // Verify balance was deducted (1000 - 300 = 700)
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances/" + voiceBalanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAvailable").value(700));
    }

    /**
     * T172: Test data usage recording with ALLOWANCE balance deduction
     * Expected: Usage record created, data balance deducted correctly (FR-096)
     */
    @Test
    void testDataUsageWithAllowanceDeduction() throws Exception {
        // Use 50 MB = 52428800 bytes from 100 MB balance
        Map<String, Object> usageRequest = createUsageRequest(
                dataBalanceId, "DATA", "STOP", BigDecimal.valueOf(52428800));
        usageRequest.put("recordOpeningTime", OffsetDateTime.now().minusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        usageRequest.put("recordClosingTime", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        usageRequest.put("bParty", "internet.apn");

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usageType").value("DATA"))
                .andExpect(jsonPath("$.volumeUsage").value(52428800))
                .andExpect(jsonPath("$.balanceValueBefore").value(104857600))
                .andExpect(jsonPath("$.balanceValueAfter").value(52428800));

        // Verify data balance was deducted (104857600 - 52428800 = 52428800)
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances/" + dataBalanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAvailable").value(52428800));
    }

    /**
     * T173: Test SMS/MMS usage recording
     * Expected: Event-based usage record created with correct balance deduction
     */
    @Test
    void testSmsUsageRecording() throws Exception {
        // Send 1 SMS from 10 SMS balance
        Map<String, Object> usageRequest = createUsageRequest(
                smsBalanceId, "SMS", "EVENT", BigDecimal.valueOf(1));
        usageRequest.put("bParty", "43664987654321");

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usageType").value("SMS"))
                .andExpect(jsonPath("$.recordType").value("EVENT"))
                .andExpect(jsonPath("$.volumeUsage").value(1))
                .andExpect(jsonPath("$.balanceValueBefore").value(10))
                .andExpect(jsonPath("$.balanceValueAfter").value(9));

        // Verify SMS balance was deducted (10 - 1 = 9)
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances/" + smsBalanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAvailable").value(9));

        // Test MMS as well
        Map<String, Object> mmsRequest = createUsageRequest(
                smsBalanceId, "MMS", "EVENT", BigDecimal.valueOf(2));
        mmsRequest.put("bParty", "43664987654321");

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mmsRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usageType").value("MMS"))
                .andExpect(jsonPath("$.balanceValueBefore").value(9))
                .andExpect(jsonPath("$.balanceValueAfter").value(7));
    }

    /**
     * T174: Test duplicate usageId validation (409 Conflict)
     * Expected: First usage succeeds, second with same usageId returns 409 (FR-092)
     */
    @Test
    void testDuplicateUsageIdValidation() throws Exception {
        String duplicateUsageId = UUID.randomUUID().toString();
        
        Map<String, Object> firstRequest = createUsageRequest(
                voiceBalanceId, "VOICE", "EVENT", BigDecimal.valueOf(30));
        firstRequest.put("usageId", duplicateUsageId);

        // First request should succeed
        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usageId").value(duplicateUsageId));

        // Second request with same usageId should fail with 409 Conflict
        Map<String, Object> secondRequest = createUsageRequest(
                voiceBalanceId, "VOICE", "EVENT", BigDecimal.valueOf(50));
        secondRequest.put("usageId", duplicateUsageId);

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * T175: Test invalid chargedPartyId (404 Not Found)
     * Expected: Request with non-existent subscriber returns 404 (FR-091)
     */
    @Test
    void testInvalidChargedPartyId() throws Exception {
        String invalidSubscriberId = UUID.randomUUID().toString();
        
        Map<String, Object> usageRequest = new HashMap<>();
        usageRequest.put("usageId", UUID.randomUUID().toString());
        usageRequest.put("chargedPartyId", invalidSubscriberId);
        usageRequest.put("usageType", "VOICE");
        usageRequest.put("recordType", "EVENT");
        usageRequest.put("volumeUsage", 60);
        usageRequest.put("impactedBalanceId", voiceBalanceId);

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usageRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Test ALLOWANCE balance floor at 0 (FR-097)
     * Expected: When volumeUsage exceeds balanceAvailable, balance is set to 0 (not negative)
     */
    @Test
    void testAllowanceBalanceFloorAtZero() throws Exception {
        // SMS balance has 10 available, try to use 15
        Map<String, Object> usageRequest = createUsageRequest(
                smsBalanceId, "SMS", "EVENT", BigDecimal.valueOf(15));

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.volumeUsage").value(15))
                .andExpect(jsonPath("$.balanceValueBefore").value(10))
                .andExpect(jsonPath("$.balanceValueAfter").value(0)); // Floor at 0

        // Verify balance is 0, not negative
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances/" + smsBalanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAvailable").value(0));
    }

    /**
     * Test COUNTER balance addition (FR-098)
     * Expected: volumeUsage is ADDED to balanceAvailable for COUNTER balance type
     */
    @Test
    void testCounterBalanceAddition() throws Exception {
        // COUNTER balance starts at 500, add 100
        Map<String, Object> usageRequest = createUsageRequest(
                counterBalanceId, "DATA", "EVENT", BigDecimal.valueOf(100));

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.volumeUsage").value(100))
                .andExpect(jsonPath("$.balanceValueBefore").value(500))
                .andExpect(jsonPath("$.balanceValueAfter").value(600)); // Added

        // Verify balance was increased (500 + 100 = 600)
        mockMvc.perform(get("/ocs/prov/v1/subscriptions/" + subscriptionId + "/balances/" + counterBalanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAvailable").value(600));
    }

    /**
     * T187: Test listing usage records for a subscriber via GET /subscribers/{subscriberId}/usage
     * Expected: 200 OK with array of usage records for the subscriber
     */
    @Test
    void testListUsageRecordsForSubscriber() throws Exception {
        // Create multiple usage records for the subscriber
        Map<String, Object> usage1 = createUsageRequest(voiceBalanceId, "VOICE", "EVENT", BigDecimal.valueOf(60));
        Map<String, Object> usage2 = createUsageRequest(dataBalanceId, "DATA", "STOP", BigDecimal.valueOf(1048576));
        Map<String, Object> usage3 = createUsageRequest(smsBalanceId, "SMS", "EVENT", BigDecimal.valueOf(1));

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usage1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usage2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/ocs/prov/v1/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usage3)))
                .andExpect(status().isCreated());

        // List all usage records for the subscriber
        mockMvc.perform(get("/ocs/prov/v1/subscribers/" + subscriberId + "/usage"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[0].chargedPartyId").value(subscriberId))
                .andExpect(jsonPath("$[0].usageId").isNotEmpty())
                .andExpect(jsonPath("$[0].usageType").exists())
                .andExpect(jsonPath("$[0].volumeUsage").exists())
                .andExpect(jsonPath("$[0].impactedBalanceId").exists());
    }

    /**
     * T188: Test usage list pagination with limit and offset parameters
     * Expected: Paginated results with correct limit and offset handling (FR-063, FR-095)
     */
    @Test
    void testUsageListPagination() throws Exception {
        // Create 5 usage records to test pagination
        for (int i = 0; i < 5; i++) {
            Map<String, Object> usage = createUsageRequest(
                    voiceBalanceId, "VOICE", "EVENT", BigDecimal.valueOf(30 + i));
            
            mockMvc.perform(post("/ocs/prov/v1/usage")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usage)))
                    .andExpect(status().isCreated());
        }

        // Test first page with limit=2
        MvcResult page1Result = mockMvc.perform(get("/ocs/prov/v1/subscribers/" + subscriberId + "/usage")
                        .param("limit", "2")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].chargedPartyId").value(subscriberId))
                .andExpect(jsonPath("$[1].chargedPartyId").value(subscriberId))
                .andReturn();

        // Test second page with limit=2, offset=2
        MvcResult page2Result = mockMvc.perform(get("/ocs/prov/v1/subscribers/" + subscriberId + "/usage")
                        .param("limit", "2")
                        .param("offset", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].chargedPartyId").value(subscriberId))
                .andExpect(jsonPath("$[1].chargedPartyId").value(subscriberId))
                .andReturn();

        // Test third page with limit=2, offset=4 (should have 1 record if exactly 5 created)
        mockMvc.perform(get("/ocs/prov/v1/subscribers/" + subscriberId + "/usage")
                        .param("limit", "2")
                        .param("offset", "4"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));

        // Test with limit=100 (max limit per FR-063)
        mockMvc.perform(get("/ocs/prov/v1/subscribers/" + subscriberId + "/usage")
                        .param("limit", "100")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(5)));

        // Test default pagination (no limit/offset params)
        mockMvc.perform(get("/ocs/prov/v1/subscribers/" + subscriberId + "/usage"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(5)));
    }

    /**
     * Helper method to create usage request with common fields
     */
    private Map<String, Object> createUsageRequest(String balanceId, String usageType, 
                                                   String recordType, BigDecimal volumeUsage) {
        Map<String, Object> request = new HashMap<>();
        request.put("usageId", UUID.randomUUID().toString());
        request.put("chargedPartyId", subscriberId);
        request.put("usageType", usageType);
        request.put("recordType", recordType);
        request.put("volumeUsage", volumeUsage);
        request.put("impactedBalanceId", balanceId);
        return request;
    }
}
