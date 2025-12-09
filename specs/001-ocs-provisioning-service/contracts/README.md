# API Contracts

## Overview

This directory contains generated API contracts from the OpenAPI specification. The contracts are generated during the Maven build process using the OpenAPI Generator Maven plugin.

## Source Specification

**OpenAPI Spec**: `app-spec/ocs-provisioing-api.yml`

This specification defines all REST endpoints, request/response schemas, error codes, and status codes for the OCS Provisioning Service.

## Generated Artifacts

The OpenAPI Generator Maven plugin generates the following artifacts during `mvn generate-sources`:

### Server Interfaces

**Location**: `target/generated-sources/openapi/src/main/java/com/telecom/ocs/provisioning/api/`

Generated Java interfaces that REST controllers must implement:
- `SubscribersApi.java` - Subscriber CRUD operations
- `SubscriptionsApi.java` - Subscription management
- `BalancesApi.java` - Balance tracking and rollover
- `GroupsApi.java` - Group management
- `NotificationAddressesApi.java` - Notification address configuration
- `TimersApi.java` - Timer scheduling
- `AccountHistoryApi.java` - Audit log operations
- `HealthCheckApi.java` - Health check endpoint

### Model DTOs

**Location**: `target/generated-sources/openapi/src/main/java/com/telecom/ocs/provisioning/model/`

Generated Data Transfer Objects (DTOs) for requests and responses:
- `Subscriber.java`, `SubscriberRequest.java`, `SubscriberResponse.java`
- `Subscription.java`, `SubscriptionRequest.java`, `SubscriptionResponse.java`
- `Balance.java`, `BalanceRequest.java`, `BalanceResponse.java`
- `Group.java`, `GroupRequest.java`, `GroupResponse.java`
- `NotificationAddress.java`, `NotificationAddressRequest.java`, `NotificationAddressResponse.java`
- `Timer.java`, `TimerRequest.java`, `TimerResponse.java`
- `AccountHistory.java`, `AccountHistoryRequest.java`, `AccountHistoryResponse.java`
- `PatchField.java` - PATCH operation field updates
- `ErrorResponse.java` - Structured error responses
- Enums: `SubscriberState.java`, `SubscriptionState.java`, `BalanceType.java`, `UnitType.java`, etc.

## Maven Configuration

Add the OpenAPI Generator plugin to `pom.xml`:

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.0.1</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/app-spec/ocs-provisioing-api.yml</inputSpec>
                <generatorName>spring</generatorName>
                <apiPackage>com.telecom.ocs.provisioning.api</apiPackage>
                <modelPackage>com.telecom.ocs.provisioning.model</modelPackage>
                <supportingFilesToGenerate>ApiUtil.java</supportingFilesToGenerate>
                <configOptions>
                    <interfaceOnly>true</interfaceOnly>
                    <skipDefaultInterface>true</skipDefaultInterface>
                    <useTags>true</useTags>
                    <dateLibrary>java8</dateLibrary>
                    <java8>true</java8>
                    <useSpringBoot3>true</useSpringBoot3>
                    <useBeanValidation>true</useBeanValidation>
                    <performBeanValidation>true</performBeanValidation>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Usage in Controllers

### 1. Implement Generated Interface

```java
@RestController
@RequestMapping("/ocs/prov/v1")
public class SubscriberController implements SubscribersApi {
    
    private final SubscriberService subscriberService;
    
    @Autowired
    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }
    
    @Override
    public ResponseEntity<SubscriberResponse> createSubscriber(
            @Valid SubscriberRequest request) {
        // Implementation here
        Subscriber subscriber = subscriberService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToResponse(subscriber));
    }
    
    @Override
    public ResponseEntity<SubscriberResponse> getSubscriber(String subscriberId) {
        // Implementation here
        Subscriber subscriber = subscriberService.findById(subscriberId);
        return ResponseEntity.ok(mapToResponse(subscriber));
    }
    
    // ... other methods
}
```

### 2. Map Between DTOs and Entities

Use MapStruct or manual mappers to convert between generated DTOs and JPA entities:

```java
@Component
public class SubscriberMapper {
    
    public Subscriber toEntity(SubscriberRequest request) {
        Subscriber entity = new Subscriber();
        entity.setMsisdn(request.getMsisdn());
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        // ... map other fields
        return entity;
    }
    
    public SubscriberResponse toResponse(Subscriber entity) {
        SubscriberResponse response = new SubscriberResponse();
        response.setSubscriberId(entity.getSubscriberId());
        response.setMsisdn(entity.getMsisdn());
        response.setState(entity.getState());
        // ... map other fields
        return response;
    }
}
```

## Regenerating Contracts

Regenerate contracts after updating OpenAPI specification:

```bash
mvn clean generate-sources
```

Or trigger full rebuild:

```bash
mvn clean install
```

## Contract Validation

### Ensure Implementation Matches Spec

1. **Interface Implementation**: Controllers MUST implement generated interfaces
2. **Bean Validation**: Use `@Valid` annotation on request parameters
3. **HTTP Status Codes**: Return codes as defined in OpenAPI spec
4. **Error Responses**: Use `ErrorResponse` DTO for all errors

### Testing Contract Compliance

Use Pact or Spring Cloud Contract for contract testing:

```java
@SpringBootTest
@AutoConfigureMockMvc
public class SubscriberContractTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void createSubscriber_shouldMatchContract() throws Exception {
        String requestJson = "{ \"msisdn\": \"43664123456789\", ... }";
        
        mockMvc.perform(post("/ocs/prov/v1/subscribers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subscriberId").exists())
            .andExpect(jsonPath("$.creationDate").exists());
    }
}
```

## Benefits of Contract-First Approach

1. **Single Source of Truth**: OpenAPI spec serves as authoritative API definition
2. **Parallel Development**: Frontend/backend teams work independently
3. **Compile-Time Safety**: Interface changes break build immediately
4. **Living Documentation**: Swagger UI auto-generated from spec
5. **Client SDK Generation**: Generate client libraries for API consumers

## Pagination Response Format

All list endpoints that support pagination (via `limit` and `offset` query parameters) MUST return responses with the following metadata structure:

```json
{
  "data": [
    { /* entity object */ },
    { /* entity object */ }
  ],
  "pagination": {
    "total": 1250,
    "limit": 20,
    "offset": 40,
    "hasNext": true,
    "hasPrevious": true,
    "nextOffset": 60,
    "previousOffset": 20
  }
}
```

**Pagination Metadata Fields**:
- `total`: Total number of records matching the query (integer)
- `limit`: Number of records requested per page (integer, 1-100)
- `offset`: Starting position in result set (integer, ≥0)
- `hasNext`: Boolean indicating if more pages exist after current page
- `hasPrevious`: Boolean indicating if pages exist before current page
- `nextOffset`: Suggested offset value for next page (null if `hasNext` is false)
- `previousOffset`: Suggested offset value for previous page (null if `hasPrevious` is false)

**Example Usage**:

```bash
# First page
GET /ocs/prov/v1/subscribers?limit=20&offset=0

# Next page
GET /ocs/prov/v1/subscribers?limit=20&offset=20

# Previous page
GET /ocs/prov/v1/subscribers?limit=20&offset=0
```

**Client Implementation Pattern**:

```java
public class PaginatedResponse<T> {
    private List<T> data;
    private PaginationMetadata pagination;
    
    // Getters and setters
}

public class PaginationMetadata {
    private Integer total;
    private Integer limit;
    private Integer offset;
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Integer nextOffset;
    private Integer previousOffset;
    
    // Getters and setters
}
```

## Swagger UI Access

Once application is running, access interactive API documentation:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON endpoint:

```
http://localhost:8080/v3/api-docs
```

## Troubleshooting

### Generation Fails

- Verify OpenAPI spec is valid YAML (use online validators)
- Check Maven plugin version compatibility with Spring Boot 3.x
- Ensure `inputSpec` path is correct in `pom.xml`

### Interface Changes Not Reflected

- Run `mvn clean` to remove stale generated code
- Rebuild with `mvn clean install`

### Import Errors in IDE

- Refresh Maven project in IDE
- Ensure `target/generated-sources/openapi` is marked as source folder
- Run `mvn generate-sources` from IDE Maven panel

## References

- OpenAPI Generator Documentation: https://openapi-generator.tech/
- OpenAPI Specification: https://spec.openapis.org/oas/v3.0.3
- Spring Boot OpenAPI Integration: https://springdoc.org/
