````markdown
# Implementation Plan: OCS Provisioning Service

**Branch**: `001-ocs-provisioning-service` | **Date**: 2025-11-07 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-ocs-provisioning-service/spec.md`

**Note**: This plan implements a containerized RESTful microservice for telecom subscriber provisioning with CRUD operations on subscribers, subscriptions, balances, groups, notifications, timers, and account history.

## Summary

Build a standalone REST microservice for telecom subscriber profile management in online charging systems. The service exposes a RESTful API conforming to OpenAPI spec `app-spec/ocs-provisioing-api.yml` with 8 core domain entities. Implementation uses Java 17, Spring Boot 3.x for application framework, Apache Camel for routing/integration patterns, Spring Data JPA for data persistence to MySQL database, and Maven for build orchestration with OpenAPI code generation. Service is containerized via Docker with multi-stage builds, orchestrated with docker-compose alongside MySQL container, and includes database schema deployment/migration scripts.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: 
  - Spring Boot 3.x (application framework, dependency injection, auto-configuration)
  - Apache Camel 4.x (routing engine, integration patterns, REST DSL)
  - Spring Data JPA 3.x (ORM abstraction, repository pattern)
  - Hibernate 6.x (JPA implementation provider)
  - MySQL Connector/J 8.x (JDBC driver)
  - OpenAPI Generator Maven Plugin (generates API interfaces from OpenAPI spec)
  - Jib Maven Plugin (containerizes application without Docker daemon)
  - Springdoc OpenAPI (generates OpenAPI docs from code)
  - Lombok (boilerplate reduction)
  - JUnit 5 (testing framework)
  - Spring Boot Test (test slices: @SpringBootTest, @DataJpaTest, @WebMvcTest)

**Storage**: MySQL 8.0+ relational database (external container) via JDBC with Spring Data JPA/Hibernate for ORM

**Testing**: 
  - Unit tests: JUnit 5 + Mockito for service layer logic
  - Integration tests: @SpringBootTest with embedded MySQL (Testcontainers) for REST endpoints
  - Repository tests: @DataJpaTest with H2 in-memory database for data access layer
  - Contract tests: OpenAPI validation against generated client/server code
  - Minimum 80% code coverage (enforced via Jacoco Maven plugin)

**Target Platform**: Linux containers (Docker) running in container orchestration platform (Kubernetes/Docker Compose)

**Project Type**: Single backend REST microservice (no frontend)

**Performance Goals**: 
  - API response time: <500ms p95 for subscriber/subscription operations
  - Lookup performance: <200ms p95 for subscriber lookups by msisdn/imsi/name
  - Throughput: 100 concurrent requests without degradation
  - Balance operations: 1000 deductions/second
  - Container startup: <30 seconds to healthy state

**Constraints**: 
  - Single-instance deployment (no clustering/HA in initial release)
  - Stateless operations (no session state)
  - ACID guarantees for all database transactions
  - OpenAPI spec compliance (100% conformance)
  - RESTful design principles (resource-oriented URLs, proper HTTP methods/status codes)

**Scale/Scope**: 
  - Data volume: millions of subscribers, tens of millions of subscriptions/balances
  - API surface: 7 domain entities × 5 CRUD operations = ~35 REST endpoints
  - Database tables: 7 (subscribers, subscriptions, balances, groups, notification_addresses, timers, account_history)
  - Source code estimate: ~8,000-10,000 LOC (excluding generated code)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| **I. API Contract First** | ✅ PASS | OpenAPI spec exists at `app-spec/ocs-provisioing-api.yml` with all endpoints, schemas, responses documented. Will use OpenAPI Generator Maven plugin to generate API interfaces before implementation. |
| **II. RESTful Design Standards** | ✅ PASS | Spec defines proper HTTP methods (GET/POST/PATCH/DELETE), status codes (200/201/204/400/404/409/422/500), resource-oriented URLs (`/subscribers/{id}/subscriptions`), pagination (limit/offset). |
| **III. Test-First Development** | ✅ PASS | Plan includes unit tests (JUnit 5), integration tests (@SpringBootTest), repository tests (@DataJpaTest), 80% coverage requirement (Jacoco). TDD workflow enforced. |
| **IV. Containerization Standards** | ✅ PASS | Plan includes Dockerfile with multi-stage build, OpenJDK 17 base image, health check endpoint (`/health-check`), docker-compose orchestration, <30s startup constraint. |
| **V. Observability and Monitoring** | ✅ PASS | Plan uses SLF4J/Logback for structured JSON logging, Spring Boot Actuator for health/metrics, correlation IDs for tracing, proper log levels. |
| **VI. Data Validation and Error Handling** | ✅ PASS | Plan uses Bean Validation (@Valid, @NotNull), @ControllerAdvice for global exception handling, structured error responses with code/message/details. |
| **Technology Stack** | ✅ PASS | Java 17, Spring Boot 3.x, Maven 3.9+, Spring Data JPA, JUnit 5, Docker all mandated by constitution and present in plan. |
| **Code Quality Standards** | ✅ PASS | Plan uses Lombok (provided scope), constructor injection pattern, SOLID principles, package structure (controllers/services/repositories/models/config/exceptions). |

**Gate Result**: ✅ ALL CHECKS PASS - Proceed to Phase 0

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

## Project Structure

### Documentation (this feature)

```text
specs/001-ocs-provisioning-service/
├── spec.md              # Feature specification (✅ complete)
├── plan.md              # This file (implementation plan)
├── research.md          # Phase 0: Technology decisions and patterns (to be generated)
├── data-model.md        # Phase 1: Entity relationships and JPA mappings (to be generated)
├── quickstart.md        # Phase 1: Build, run, test instructions (to be generated)
├── contracts/           # Phase 1: Generated OpenAPI artifacts
│   ├── api-interfaces/  # Generated REST controller interfaces
│   └── model-dtos/      # Generated request/response DTOs
└── tasks.md             # Phase 2: Task breakdown (not created by /speckit.plan)
```

### Source Code (repository root)

```text
mcp-prov-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/telecom/ocs/provisioning/
│   │   │       ├── OcsProvisioningApplication.java    # Spring Boot main class
│   │   │       ├── config/                             # Configuration classes
│   │   │       │   ├── CamelConfig.java               # Apache Camel route configuration
│   │   │       │   ├── DataSourceConfig.java          # MySQL datasource setup
│   │   │       │   ├── OpenApiConfig.java             # Springdoc OpenAPI customization
│   │   │       │   └── GlobalExceptionHandler.java    # @ControllerAdvice error handling
│   │   │       ├── controllers/                        # REST controllers (implement generated interfaces)
│   │   │       │   ├── SubscriberController.java
│   │   │       │   ├── SubscriptionController.java
│   │   │       │   ├── BalanceController.java
│   │   │       │   ├── GroupController.java
│   │   │       │   ├── NotificationAddressController.java
│   │   │       │   ├── TimerController.java
│   │   │       │   ├── AccountHistoryController.java
│   │   │       │   ├── UsageController.java
│   │   │       │   └── HealthCheckController.java
│   │   │       ├── services/                           # Business logic layer
│   │   │       │   ├── SubscriberService.java
│   │   │       │   ├── SubscriptionService.java
│   │   │       │   ├── BalanceService.java
│   │   │       │   ├── GroupService.java
│   │   │       │   ├── NotificationAddressService.java
│   │   │       │   ├── TimerService.java
│   │   │       │   ├── AccountHistoryService.java
│   │   │       │   └── UsageService.java
│   │   │       ├── repositories/                       # Spring Data JPA repositories
│   │   │       │   ├── SubscriberRepository.java
│   │   │       │   ├── SubscriptionRepository.java
│   │   │       │   ├── BalanceRepository.java
│   │   │       │   ├── GroupRepository.java
│   │   │       │   ├── NotificationAddressRepository.java
│   │   │       │   ├── TimerRepository.java
│   │   │       │   ├── AccountHistoryRepository.java
│   │   │       │   └── UsageRepository.java
│   │   │       ├── models/                             # JPA entity classes
│   │   │       │   ├── Subscriber.java
│   │   │       │   ├── Subscription.java
│   │   │       │   ├── Balance.java
│   │   │       │   ├── Group.java
│   │   │       │   ├── NotificationAddress.java
│   │   │       │   ├── Timer.java
│   │   │       │   ├── AccountHistory.java
│   │   │       │   └── Usage.java
│   │   │       ├── dto/                                # Data Transfer Objects
│   │   │       │   ├── requests/                       # Request payloads
│   │   │       │   └── responses/                      # Response payloads
│   │   │       ├── exceptions/                         # Custom exception classes
│   │   │       │   ├── ResourceNotFoundException.java
│   │   │       │   ├── DuplicateResourceException.java
│   │   │       │   ├── ValidationException.java
│   │   │       │   └── OptimisticLockingException.java
│   │   │       ├── mappers/                            # Entity ↔ DTO converters
│   │   │       │   ├── SubscriberMapper.java
│   │   │       │   └── ...
│   │   │       └── routes/                             # Apache Camel route definitions
│   │   │           └── ProvisioningRoutes.java        # Camel REST DSL routes
│   │   └── resources/
│   │       ├── application.yml                         # Spring Boot configuration
│   │       ├── application-dev.yml                     # Dev profile
│   │       ├── application-test.yml                    # Test profile
│   │       ├── application-prod.yml                    # Production profile
│   │       ├── logback-spring.xml                      # Logging configuration
│   │       └── db/
│   │           └── migration/                          # Flyway database migrations
│   │               ├── V1__initial_schema.sql
│   │               └── V2__add_indexes.sql
│   └── test/
│       └── java/
│           └── com/telecom/ocs/provisioning/
│               ├── integration/                        # @SpringBootTest integration tests
│               │   ├── SubscriberIntegrationTest.java
│               │   └── ...
│               ├── repository/                         # @DataJpaTest repository tests
│               │   ├── SubscriberRepositoryTest.java
│               │   └── ...
│               └── service/                            # Unit tests with Mockito
│                   ├── SubscriberServiceTest.java
│                   └── ...
├── pom.xml                                             # Maven build configuration
├── Dockerfile                                          # Multi-stage Docker build
├── docker-compose.yml                                  # Service + MySQL orchestration
├── .dockerignore                                       # Docker build exclusions
├── scripts/
│   ├── deploy-schema.sh                                # Database schema deployment
│   └── migrate-schema.sh                               # Database schema migration
└── README.md                                           # Project overview and setup
```

**Structure Decision**: Single backend REST microservice structure chosen. No frontend component required. All REST endpoints exposed via Spring MVC controllers implementing OpenAPI-generated interfaces. Apache Camel routes provide additional integration patterns and can act as alternative REST DSL (optional enhancement). Standard Maven project layout with clear separation of concerns: controllers (API layer) → services (business logic) → repositories (data access) → models (JPA entities).

## Phase 0: Outline & Research

This specification had no unresolved clarifications. Phase 0 documents concrete technology choices, patterns, and tooling to de-risk implementation.

Decisions:
- Database: MySQL 8.0 (containerized) with UTF8MB4 collation
- Migrations: Flyway (versioned SQL under `src/main/resources/db/migration`)
- Testing DB: Testcontainers (MySQL module) for integration tests; H2 for repository unit tests where feasible
- OpenAPI: Use OpenAPI Generator Maven plugin to generate server interfaces and DTOs from `app-spec/ocs-provisioing-api.yml`
- Containerization: Multi-stage Dockerfile (Temurin JDK 17 builder + Temurin JRE 17 runtime)
- Observability: Logback JSON encoder, Spring Boot Actuator (health, metrics, info)
- Concurrency control: Optimistic locking via `@Version` and `lastModifiedDate` checks as per FR-072

Research tasks captured in `research.md` (generated in this run) consolidate rationale and alternatives.

## Phase 1: Design & Contracts

Deliverables created in this run:
- `research.md` — consolidated decisions and alternatives
- `data-model.md` — entity fields, relationships, constraints, and JPA mapping notes
- `contracts/README.md` — how to generate interfaces/DTOs from OpenAPI
- `quickstart.md` — build, test, and run instructions (Maven, Docker, docker-compose)

Design notes:
- Entities map one-to-one with domain (Subscriber, Subscription, Balance, Group, NotificationAddress, Timer, AccountHistory, Usage)
- REST controllers implement generated interfaces for contract-first adherence
- Apache Camel routes encapsulate cross-cutting integration patterns; REST remains Spring MVC
- Validation via Bean Validation annotations on DTOs; global exception handler provides structured errors

## Outputs & Next Steps

Branch: `001-ocs-provisioning-service`  
Plan: `specs/001-ocs-provisioning-service/plan.md`  
Generated: `research.md`, `data-model.md`, `contracts/README.md`, `quickstart.md`

Next command suggested: `/speckit.tasks` to produce executable task breakdown, or proceed to scaffolding code per plan.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
