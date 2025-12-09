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

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Subscriber API endpoints.
 * Tests the complete REST API flow using Testcontainers MySQL.
 * 
 * Tests:
 * - T039: POST /subscribers (create subscriber)
 * - T040: GET /subscribers/{id} (retrieve subscriber)
 * - T041: GET /subscribers/lookup (lookup by msisdn/imsi/name)
 * - T042: PATCH /subscribers/{id} (update subscriber state)
 * - T043: DELETE /subscribers/{id} (delete subscriber)
 * - T044: Duplicate msisdn validation (409 Conflict)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class SubscriberIntegrationTest {

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

    private Map<String, Object> validSubscriberRequest;

    @BeforeEach
    void setUp() {
        // Prepare valid subscriber request payload
        validSubscriberRequest = new HashMap<>();
        validSubscriberRequest.put("msisdn", "43664123456789");
        validSubscriberRequest.put("imsi", "214010123456789");
        validSubscriberRequest.put("firstName", "John");
        validSubscriberRequest.put("lastName", "Doe");
        validSubscriberRequest.put("dateOfBirth", "1990-01-15");
        validSubscriberRequest.put("email", "john.doe@example.com");
        validSubscriberRequest.put("contactNumber", "436641234567");
        validSubscriberRequest.put("billingCycle", 1);
        validSubscriberRequest.put("billingAddress", "123 Main St, Vienna, Austria");
    }

    /**
     * T039: Test creating a new subscriber via POST /subscribers
     * Expected: 201 Created with subscriber data including generated subscriberId
     */
    @Test
    void testCreateSubscriber() throws Exception {
        String requestJson = objectMapper.writeValueAsString(validSubscriberRequest);

        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subscriberId").isNotEmpty())
                .andExpect(jsonPath("$.msisdn").value("43664123456789"))
                .andExpect(jsonPath("$.imsi").value("214010123456789"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.state").value("PRE_PROVISIONED"))
                .andExpect(jsonPath("$.creationDate").isNotEmpty())
                .andExpect(jsonPath("$.lastModifiedDate").isNotEmpty());
    }

    /**
     * T039 (additional): Test creating subscriber with minimal required fields
     * Expected: 201 Created with only msisdn provided
     */
    @Test
    void testCreateSubscriberMinimalFields() throws Exception {
        Map<String, Object> minimalRequest = new HashMap<>();
        minimalRequest.put("msisdn", "43664987654321");

        String requestJson = objectMapper.writeValueAsString(minimalRequest);

        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriberId").isNotEmpty())
                .andExpect(jsonPath("$.msisdn").value("43664987654321"))
                .andExpect(jsonPath("$.state").value("PRE_PROVISIONED"));
    }

    /**
     * T039 (validation): Test creating subscriber with invalid msisdn format
     * Expected: 400 Bad Request
     */
    @Test
    void testCreateSubscriberInvalidMsisdn() throws Exception {
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("msisdn", "123"); // Too short, invalid format

        String requestJson = objectMapper.writeValueAsString(invalidRequest);

        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    /**
     * T039 (validation): Test creating subscriber with missing required field
     * Expected: 400 Bad Request
     */
    @Test
    void testCreateSubscriberMissingMsisdn() throws Exception {
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("firstName", "Jane");
        // Missing required msisdn field

        String requestJson = objectMapper.writeValueAsString(invalidRequest);

        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    /**
     * T040: Test retrieving subscriber by ID via GET /subscribers/{id}
     * Expected: 200 OK with subscriber data
     */
    @Test
    void testGetSubscriberById() throws Exception {
        // First create a subscriber
        String requestJson = objectMapper.writeValueAsString(validSubscriberRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscriber = objectMapper.readValue(createResponse, Map.class);
        String subscriberId = (String) createdSubscriber.get("subscriberId");

        // Now retrieve the subscriber by ID
        mockMvc.perform(get("/ocs/prov/v1/subscribers/" + subscriberId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subscriberId").value(subscriberId))
                .andExpect(jsonPath("$.msisdn").value("43664123456789"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    /**
     * T040 (error case): Test retrieving non-existent subscriber
     * Expected: 404 Not Found
     */
    @Test
    void testGetSubscriberByIdNotFound() throws Exception {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        
        mockMvc.perform(get("/ocs/prov/v1/subscribers/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    /**
     * T041: Test looking up subscriber by msisdn via GET /subscribers/lookup?msisdn={msisdn}
     * Expected: 200 OK with subscriber data
     */
    @Test
    void testLookupSubscriberByMsisdn() throws Exception {
        // First create a subscriber
        String requestJson = objectMapper.writeValueAsString(validSubscriberRequest);
        
        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Now lookup by msisdn
        mockMvc.perform(get("/ocs/prov/v1/subscribers/lookup")
                        .param("msisdn", "43664123456789"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.msisdn").value("43664123456789"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    /**
     * T041: Test looking up subscriber by imsi via GET /subscribers/lookup?imsi={imsi}
     * Expected: 200 OK with subscriber data
     */
    @Test
    void testLookupSubscriberByImsi() throws Exception {
        // First create a subscriber
        String requestJson = objectMapper.writeValueAsString(validSubscriberRequest);
        
        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Now lookup by imsi
        mockMvc.perform(get("/ocs/prov/v1/subscribers/lookup")
                        .param("imsi", "214010123456789"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.imsi").value("214010123456789"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    /**
     * T041: Test looking up subscriber by name via GET /subscribers/lookup?firstName={name}&lastName={name}
     * Expected: 200 OK with subscriber data
     */
    @Test
    void testLookupSubscriberByName() throws Exception {
        // First create a subscriber
        String requestJson = objectMapper.writeValueAsString(validSubscriberRequest);
        
        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Now lookup by name
        mockMvc.perform(get("/ocs/prov/v1/subscribers/lookup")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    /**
     * T041 (error case): Test looking up non-existent subscriber
     * Expected: 404 Not Found
     */
    @Test
    void testLookupSubscriberNotFound() throws Exception {
        mockMvc.perform(get("/ocs/prov/v1/subscribers/lookup")
                        .param("msisdn", "99999999999999"))
                .andExpect(status().isNotFound());
    }

    /**
     * T042: Test updating subscriber state via PATCH /subscribers/{id}
     * Expected: 200 OK with updated state, previousState, and lastTransitionDate
     */
    @Test
    void testUpdateSubscriberState() throws Exception {
        // First create a subscriber
        String createRequestJson = objectMapper.writeValueAsString(validSubscriberRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscriber = objectMapper.readValue(createResponse, Map.class);
        String subscriberId = (String) createdSubscriber.get("subscriberId");

        // Now update the state to ACTIVE
        Map<String, Object> patchRequest = new HashMap<>();
        patchRequest.put("fieldName", "state");
        patchRequest.put("fieldValue", "ACTIVE");
        
        String patchRequestJson = objectMapper.writeValueAsString(new Map[] { patchRequest });

        mockMvc.perform(patch("/ocs/prov/v1/subscribers/" + subscriberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriberId").value(subscriberId))
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.previousState").value("PRE_PROVISIONED"))
                .andExpect(jsonPath("$.lastTransitionDate").isNotEmpty());
    }

    /**
     * T042 (validation): Test updating subscriber with invalid field
     * Expected: 400 Bad Request or 422 Unprocessable Entity
     */
    @Test
    void testUpdateSubscriberInvalidField() throws Exception {
        // First create a subscriber
        String createRequestJson = objectMapper.writeValueAsString(validSubscriberRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscriber = objectMapper.readValue(createResponse, Map.class);
        String subscriberId = (String) createdSubscriber.get("subscriberId");

        // Try to update with invalid field name
        Map<String, Object> patchRequest = new HashMap<>();
        patchRequest.put("fieldName", "invalidField");
        patchRequest.put("fieldValue", "someValue");
        
        String patchRequestJson = objectMapper.writeValueAsString(new Map[] { patchRequest });

        mockMvc.perform(patch("/ocs/prov/v1/subscribers/" + subscriberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequestJson))
                .andExpect(status().is4xxClientError());
    }

    /**
     * T043: Test deleting subscriber via DELETE /subscribers/{id}
     * Expected: 204 No Content and subscriber is deleted
     */
    @Test
    void testDeleteSubscriber() throws Exception {
        // First create a subscriber
        String requestJson = objectMapper.writeValueAsString(validSubscriberRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscriber = objectMapper.readValue(createResponse, Map.class);
        String subscriberId = (String) createdSubscriber.get("subscriberId");

        // Delete the subscriber
        mockMvc.perform(delete("/ocs/prov/v1/subscribers/" + subscriberId))
                .andExpect(status().isNoContent());

        // Verify subscriber is deleted
        mockMvc.perform(get("/ocs/prov/v1/subscribers/" + subscriberId))
                .andExpect(status().isNotFound());
    }

    /**
     * T043 (error case): Test deleting non-existent subscriber
     * Expected: 404 Not Found
     */
    @Test
    void testDeleteSubscriberNotFound() throws Exception {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        
        mockMvc.perform(delete("/ocs/prov/v1/subscribers/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    /**
     * T044: Test duplicate msisdn validation
     * Expected: 409 Conflict when creating subscriber with duplicate msisdn
     */
    @Test
    void testCreateSubscriberDuplicateMsisdn() throws Exception {
        // First create a subscriber
        String requestJson = objectMapper.writeValueAsString(validSubscriberRequest);
        
        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Try to create another subscriber with the same msisdn
        Map<String, Object> duplicateRequest = new HashMap<>(validSubscriberRequest);
        duplicateRequest.put("firstName", "Jane"); // Different name but same msisdn
        
        String duplicateRequestJson = objectMapper.writeValueAsString(duplicateRequest);

        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequestJson))
                .andExpect(status().isConflict());
    }

    /**
     * T044 (edge case): Test that deactivated subscribers can reuse msisdn
     * Expected: 201 Created (msisdn uniqueness only for active states)
     */
    @Test
    void testCreateSubscriberReuseMsisdnAfterDeactivation() throws Exception {
        // First create and activate a subscriber
        String requestJson = objectMapper.writeValueAsString(validSubscriberRequest);
        
        MvcResult createResult = mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdSubscriber = objectMapper.readValue(createResponse, Map.class);
        String subscriberId = (String) createdSubscriber.get("subscriberId");

        // Deactivate the subscriber
        Map<String, Object> patchRequest = new HashMap<>();
        patchRequest.put("fieldName", "state");
        patchRequest.put("fieldValue", "DEACTIVATED");
        
        String patchRequestJson = objectMapper.writeValueAsString(new Map[] { patchRequest });

        mockMvc.perform(patch("/ocs/prov/v1/subscribers/" + subscriberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequestJson))
                .andExpect(status().isOk());

        // Now create a new subscriber with the same msisdn (should be allowed)
        Map<String, Object> newRequest = new HashMap<>(validSubscriberRequest);
        newRequest.put("firstName", "Jane"); // Different subscriber
        
        String newRequestJson = objectMapper.writeValueAsString(newRequest);

        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newRequestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.msisdn").value("43664123456789"));
    }
}
