# Tasks: OCS Provisioning Service

**Input**: Design documents from `/specs/001-ocs-provisioning-service/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Tests are included per constitution requirement (Test-First Development principle). Write tests FIRST, ensure they FAIL before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Single backend REST microservice structure:
- Source: `src/main/java/com/telecom/ocs/provisioning/`
- Tests: `src/test/java/com/telecom/ocs/provisioning/`
- Resources: `src/main/resources/`
- Database migrations: `src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Create Maven project structure with groupId com.telecom.ocs.provisioning in pom.xml
- [x] T002 Configure Spring Boot 3.x parent and dependencies in pom.xml
- [x] T003 [P] Add Spring Data JPA, MySQL Connector/J, Hibernate dependencies to pom.xml
- [x] T004 [P] Add Apache Camel Spring Boot starter dependencies to pom.xml
- [x] T005 [P] Add OpenAPI Generator Maven plugin configuration to pom.xml
- [x] T006 [P] Add Jib Maven plugin for Docker containerization to pom.xml
- [x] T007 [P] Add JUnit 5, Mockito, Spring Boot Test, Testcontainers dependencies to pom.xml
- [x] T008 [P] Add Jacoco Maven plugin for code coverage (80% minimum) to pom.xml
- [x] T009 [P] Add Lombok dependency (provided scope) to pom.xml
- [x] T010 [P] Add Springdoc OpenAPI dependency to pom.xml
- [x] T011 Create Spring Boot main application class in src/main/java/com/telecom/ocs/provisioning/OcsProvisioningApplication.java
- [x] T012 [P] Create application.yml with Spring profiles (dev/test/prod) in src/main/resources/
- [x] T013 [P] Create application-dev.yml with development settings in src/main/resources/
- [x] T014 [P] Create application-test.yml with test settings in src/main/resources/
- [x] T015 [P] Create application-prod.yml with production settings in src/main/resources/
- [x] T016 [P] Create logback-spring.xml for structured JSON logging in src/main/resources/
- [x] T017 Create README.md with project overview and quickstart instructions

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T018 Create DataSourceConfig.java for MySQL datasource configuration in src/main/java/com/telecom/ocs/provisioning/config/
- [x] T019 [P] Create OpenApiConfig.java for Springdoc customization in src/main/java/com/telecom/ocs/provisioning/config/
- [x] T020 [P] Create CamelConfig.java for Apache Camel route configuration in src/main/java/com/telecom/ocs/provisioning/config/
- [x] T021 Create GlobalExceptionHandler.java with @ControllerAdvice in src/main/java/com/telecom/ocs/provisioning/config/
- [x] T022 [P] Create custom exception classes: ResourceNotFoundException in src/main/java/com/telecom/ocs/provisioning/exceptions/
- [x] T023 [P] Create custom exception classes: DuplicateResourceException in src/main/java/com/telecom/ocs/provisioning/exceptions/
- [x] T024 [P] Create custom exception classes: ValidationException in src/main/java/com/telecom/ocs/provisioning/exceptions/
- [x] T025 [P] Create custom exception classes: OptimisticLockingException in src/main/java/com/telecom/ocs/provisioning/exceptions/
- [x] T026 Create ErrorResponse DTO structure for structured error responses in src/main/java/com/telecom/ocs/provisioning/dto/responses/
- [x] T027 Configure Flyway database migrations framework in pom.xml and application.yml
- [x] T028 Create V1__initial_schema.sql with all 7 entity tables in src/main/resources/db/migration/
- [x] T029 Create V2__add_indexes.sql with performance indexes in src/main/resources/db/migration/
- [x] T030 Run OpenAPI Generator Maven plugin to generate API interfaces from app-spec/ocs-provisioing-api.yml
- [x] T031 Create HealthCheckController implementing health check endpoint in src/main/java/com/telecom/ocs/provisioning/controllers/
- [x] T032 Create Dockerfile with multi-stage build (Temurin JDK 17 builder, JRE 17 runtime)
- [x] T033 Create docker-compose.yml orchestrating REST service and MySQL containers
- [x] T034 Create deploy-schema.sh script in scripts/ directory
- [x] T035 Create migrate-schema.sh script in scripts/ directory
- [x] T036 Configure Spring Boot Actuator endpoints in application.yml
- [x] T037 [P] Validate external RDBMS connection configuration in application.yml profiles (FR-076 compliance)
- [x] T038 [P] Validate environment variable externalization for database connection, ports, and service config (FR-079 compliance)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Subscriber Profile Management (Priority: P1) 🎯 MVP

**Goal**: Enable telecom operators to create, retrieve, update, and delete subscriber profiles with personal information, billing details, service entitlements, and lifecycle state management.

**Independent Test**: Create subscriber with msisdn and personal info, retrieve by subscriberId or lookup criteria (msisdn/imsi/name), update state transitions, delete subscriber with cascade handling.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T039 [P] [US1] Write integration test for POST /subscribers (create subscriber) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriberIntegrationTest.java
- [x] T040 [P] [US1] Write integration test for GET /subscribers/{id} (retrieve subscriber) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriberIntegrationTest.java
- [x] T041 [P] [US1] Write integration test for GET /subscribers/lookup (lookup by msisdn/imsi/name) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriberIntegrationTest.java
- [x] T042 [P] [US1] Write integration test for PATCH /subscribers/{id} (update subscriber state) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriberIntegrationTest.java
- [x] T043 [P] [US1] Write integration test for DELETE /subscribers/{id} (delete subscriber) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriberIntegrationTest.java
- [x] T044 [P] [US1] Write integration test for duplicate msisdn validation (409 Conflict) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriberIntegrationTest.java
- [x] T045 [P] [US1] Write repository test for SubscriberRepository in src/test/java/com/telecom/ocs/provisioning/repository/SubscriberRepositoryTest.java
- [x] T046 [P] [US1] Write unit test for SubscriberService with Mockito in src/test/java/com/telecom/ocs/provisioning/service/SubscriberServiceTest.java

### Implementation for User Story 1

- [x] T047 [US1] Create Subscriber JPA entity with @Entity, lifecycle states, validation annotations in src/main/java/com/telecom/ocs/provisioning/models/Subscriber.java
- [x] T048 [US1] Create SubscriberRepository extending JpaRepository with custom queries in src/main/java/com/telecom/ocs/provisioning/repositories/SubscriberRepository.java
- [x] T049 [US1] Implement SubscriberService with CRUD operations, state transitions, cascade delete logic in src/main/java/com/telecom/ocs/provisioning/services/SubscriberService.java
- [x] T050 [US1] Create SubscriberMapper for entity ↔ DTO conversion in src/main/java/com/telecom/ocs/provisioning/mappers/SubscriberMapper.java
- [x] T051 [US1] Implement SubscriberController implementing generated SubscribersApi interface in src/main/java/com/telecom/ocs/provisioning/controllers/SubscriberController.java
- [x] T052 [US1] Add msisdn format validation (@Pattern annotation) and uniqueness constraint handling
- [x] T053 [US1] Add previousState and lastTransitionDate update logic on state changes
- [x] T054 [US1] Add logging for subscriber lifecycle operations (creation, state transitions, deletion)

**Checkpoint**: User Story 1 complete and independently testable - MVP ready for deployment

---

## Phase 4: User Story 2 - Subscription Lifecycle Management (Priority: P1)

**Goal**: Enable operators to provision and manage service subscriptions for subscribers, including offers, recurring billing cycles, and subscription states.

**Independent Test**: Create subscription under subscriber, activate/suspend, manage recurring cycles, verify automatic expiration at maxRecurringCycles, delete with cascade to balances.

### Tests for User Story 2

- [x] T055 [P] [US2] Write integration test for POST /subscribers/{id}/subscriptions (create subscription) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriptionIntegrationTest.java
- [x] T056 [P] [US2] Write integration test for GET /subscriptions/{id} (retrieve subscription) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriptionIntegrationTest.java
- [x] T057 [P] [US2] Write integration test for GET /subscribers/{id}/subscriptions (list subscriptions) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriptionIntegrationTest.java
- [x] T058 [P] [US2] Write integration test for PATCH /subscriptions/{id} (activate/cancel subscription) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriptionIntegrationTest.java
- [x] T059 [P] [US2] Write integration test for DELETE /subscriptions/{id} (delete subscription) in src/test/java/com/telecom/ocs/provisioning/integration/SubscriptionIntegrationTest.java
- [x] T060 [P] [US2] Write integration test for recurring cycle expiration logic in src/test/java/com/telecom/ocs/provisioning/integration/SubscriptionIntegrationTest.java
- [x] T061 [P] [US2] Write repository test for SubscriptionRepository in src/test/java/com/telecom/ocs/provisioning/repository/SubscriptionRepositoryTest.java
- [x] T062 [P] [US2] Write unit test for SubscriptionService with Mockito in src/test/java/com/telecom/ocs/provisioning/service/SubscriptionServiceTest.java

### Implementation for User Story 2

- [x] T063 [US2] Create Subscription JPA entity with @ManyToOne to Subscriber, lifecycle states in src/main/java/com/telecom/ocs/provisioning/models/Subscription.java
- [x] T064 [US2] Create SubscriptionRepository extending JpaRepository with findBySubscriberId query in src/main/java/com/telecom/ocs/provisioning/repositories/SubscriptionRepository.java
- [x] T065 [US2] Implement SubscriptionService with recurring cycle logic, auto-expiration in src/main/java/com/telecom/ocs/provisioning/services/SubscriptionService.java
- [x] T066 [US2] Create SubscriptionMapper for entity ↔ DTO conversion in src/main/java/com/telecom/ocs/provisioning/mappers/SubscriptionMapper.java
- [x] T067 [US2] Implement SubscriptionController implementing generated SubscriptionsApi interface in src/main/java/com/telecom/ocs/provisioning/controllers/SubscriptionController.java
- [x] T068 [US2] Add renewalDate calculation logic based on cycleLengthType and cycleLengthUnits (implemented in Subscription entity and SubscriptionService)
- [x] T069 [US2] Add automatic state transition to EXPIRED when recurringCyclesCompleted == maxRecurringCycles (implemented in Subscription.incrementRecurringCycle())
- [x] T070 [US2] Add logging for subscription lifecycle operations (implemented with @Slf4j in SubscriptionService and SubscriptionController)

**Checkpoint**: User Stories 1 AND 2 complete and independently testable

---

## Phase 5: User Story 3 - Balance Tracking and Rollover (Priority: P1)

**Goal**: Enable operators to track usage balances (data allowances, voice minutes, monetary credits) for subscriptions with expiration, rollover handling, and recurring cycle management.

**Independent Test**: Create balance under subscription, track balanceAmount vs balanceAvailable, handle rollover at cycle boundaries (capped at maxRolloverAmount), enforce expiration dates, support group balances.

### Tests for User Story 3

- [x] T071 [P] [US3] Write integration test for POST /subscriptions/{id}/balances (create balance) in src/test/java/com/telecom/ocs/provisioning/integration/BalanceIntegrationTest.java
- [x] T072 [P] [US3] Write integration test for GET /balances/{id} (retrieve balance) in src/test/java/com/telecom/ocs/provisioning/integration/BalanceIntegrationTest.java
- [x] T073 [P] [US3] Write integration test for GET /subscriptions/{id}/balances (list balances) in src/test/java/com/telecom/ocs/provisioning/integration/BalanceIntegrationTest.java
- [x] T074 [P] [US3] Write integration test for balance rollover logic (cap at maxRolloverAmount) in src/test/java/com/telecom/ocs/provisioning/integration/BalanceIntegrationTest.java
- [x] T075 [P] [US3] Write integration test for balance expiration enforcement in src/test/java/com/telecom/ocs/provisioning/integration/BalanceIntegrationTest.java
- [x] T076 [P] [US3] Write integration test for group balance sharing in src/test/java/com/telecom/ocs/provisioning/integration/BalanceIntegrationTest.java
- [x] T077 [P] [US3] Write repository test for BalanceRepository in src/test/java/com/telecom/ocs/provisioning/repository/BalanceRepositoryTest.java
- [x] T078 [P] [US3] Write unit test for BalanceService with Mockito in src/test/java/com/telecom/ocs/provisioning/service/BalanceServiceTest.java

### Implementation for User Story 3

- [x] T079 [US3] Create Balance JPA entity with @ManyToOne to Subscription, balanceType/unitType enums in src/main/java/com/telecom/ocs/provisioning/models/Balance.java
- [x] T080 [US3] Create BalanceRepository extending JpaRepository with findBySubscriptionId query in src/main/java/com/telecom/ocs/provisioning/repositories/BalanceRepository.java
- [x] T081 [US3] Implement BalanceService with rollover logic, expiration checking in src/main/java/com/telecom/ocs/provisioning/services/BalanceService.java
- [x] T082 [US3] Create BalanceMapper for entity ↔ DTO conversion in src/main/java/com/telecom/ocs/provisioning/mappers/BalanceMapper.java
- [x] T083 [US3] Implement BalanceController implementing generated BalancesApi interface in src/main/java/com/telecom/ocs/provisioning/controllers/BalanceController.java
- [x] T084 [US3] Add rollover amount capping logic (rolloverAmount <= maxRolloverAmount)
- [x] T085 [US3] Add expiration date validation against current date
- [x] T086 [US3] Add group balance sharing logic (isGroupBalance flag handling)
- [x] T087 [US3] Add logging for balance operations

**Checkpoint**: Core P1 user stories (Subscriber, Subscription, Balance) complete - Core charging system functionality ready

---

## Phase 6: User Story 7 - Account History Tracking (Priority: P2)

**Goal**: Enable operators to track all interactions and events on subscriber accounts, groups, and related entities for audit trails, compliance, and customer service inquiries.

**Independent Test**: Record interactions on entities with timestamps, query history by entityId, retrieve specific interactions by interactionId, verify chronological ordering.

### Tests for User Story 7

- [ ] T088 [P] [US7] Write integration test for POST /account-history (create history entry) in src/test/java/com/telecom/ocs/provisioning/integration/AccountHistoryIntegrationTest.java
- [ ] T089 [P] [US7] Write integration test for GET /account-history/{id} (retrieve history entry) in src/test/java/com/telecom/ocs/provisioning/integration/AccountHistoryIntegrationTest.java
- [ ] T090 [P] [US7] Write integration test for GET /account-history?entityId={id} (list history by entity) in src/test/java/com/telecom/ocs/provisioning/integration/AccountHistoryIntegrationTest.java
- [ ] T091 [P] [US7] Write integration test for chronological ordering of history entries in src/test/java/com/telecom/ocs/provisioning/integration/AccountHistoryIntegrationTest.java
- [ ] T092 [P] [US7] Write repository test for AccountHistoryRepository in src/test/java/com/telecom/ocs/provisioning/repository/AccountHistoryRepositoryTest.java
- [ ] T093 [P] [US7] Write unit test for AccountHistoryService with Mockito in src/test/java/com/telecom/ocs/provisioning/service/AccountHistoryServiceTest.java

### Implementation for User Story 7

- [ ] T094 [US7] Create AccountHistory JPA entity with entityType enum, attachment metadata in src/main/java/com/telecom/ocs/provisioning/models/AccountHistory.java
- [ ] T095 [US7] Create AccountHistoryRepository extending JpaRepository with findByEntityIdOrderByStartDateTimeDesc query in src/main/java/com/telecom/ocs/provisioning/repositories/AccountHistoryRepository.java
- [ ] T096 [US7] Implement AccountHistoryService with audit logging logic in src/main/java/com/telecom/ocs/provisioning/services/AccountHistoryService.java
- [ ] T097 [US7] Create AccountHistoryMapper for entity ↔ DTO conversion in src/main/java/com/telecom/ocs/provisioning/mappers/AccountHistoryMapper.java
- [ ] T098 [US7] Implement AccountHistoryController implementing generated AccountHistoryApi interface in src/main/java/com/telecom/ocs/provisioning/controllers/AccountHistoryController.java
- [ ] T099 [US7] Add automatic history entry creation on subscriber/subscription state transitions
- [ ] T100 [US7] Add pagination support for history list queries (limit/offset)
- [ ] T101 [US7] Add logging for audit history operations

**Checkpoint**: P2 audit functionality complete - Compliance and troubleshooting capabilities enabled

---

## Phase 7: User Story 4 - Group Management (Priority: P2)

**Goal**: Enable operators to create subscriber groups for family plans, corporate accounts, or shared subscriptions where multiple subscribers share quotas and services under a group owner.

**Independent Test**: Create group with group owner, add/remove members, enforce maxMembers limit, manage group subscriptions, terminate group and unlink members.

### Tests for User Story 4

- [ ] T102 [P] [US4] Write integration test for POST /groups (create group) in src/test/java/com/telecom/ocs/provisioning/integration/GroupIntegrationTest.java
- [ ] T103 [P] [US4] Write integration test for GET /groups/{id} (retrieve group) in src/test/java/com/telecom/ocs/provisioning/integration/GroupIntegrationTest.java
- [ ] T104 [P] [US4] Write integration test for POST /groups/{id}/members (add member) in src/test/java/com/telecom/ocs/provisioning/integration/GroupIntegrationTest.java
- [ ] T105 [P] [US4] Write integration test for DELETE /groups/{id}/members/{subscriberId} (remove member) in src/test/java/com/telecom/ocs/provisioning/integration/GroupIntegrationTest.java
- [ ] T106 [P] [US4] Write integration test for maxMembers limit enforcement in src/test/java/com/telecom/ocs/provisioning/integration/GroupIntegrationTest.java
- [ ] T107 [P] [US4] Write integration test for group owner removal prevention in src/test/java/com/telecom/ocs/provisioning/integration/GroupIntegrationTest.java
- [ ] T108 [P] [US4] Write repository test for GroupRepository in src/test/java/com/telecom/ocs/provisioning/repository/GroupRepositoryTest.java
- [ ] T109 [P] [US4] Write unit test for GroupService with Mockito in src/test/java/com/telecom/ocs/provisioning/service/GroupServiceTest.java

### Implementation for User Story 4

- [ ] T110 [US4] Create Group JPA entity with groupOwner @ManyToOne, members JSON/array in src/main/java/com/telecom/ocs/provisioning/models/Group.java
- [ ] T111 [US4] Create GroupRepository extending JpaRepository with findByGroupName query in src/main/java/com/telecom/ocs/provisioning/repositories/GroupRepository.java
- [ ] T112 [US4] Implement GroupService with member management, maxMembers validation in src/main/java/com/telecom/ocs/provisioning/services/GroupService.java
- [ ] T113 [US4] Create GroupMapper for entity ↔ DTO conversion in src/main/java/com/telecom/ocs/provisioning/mappers/GroupMapper.java
- [ ] T114 [US4] Implement GroupController implementing generated GroupsApi interface in src/main/java/com/telecom/ocs/provisioning/controllers/GroupController.java
- [ ] T115 [US4] Add maxMembers limit enforcement logic
- [ ] T116 [US4] Add group owner removal prevention logic
- [ ] T117 [US4] Add memberSince timestamp recording on member addition
- [ ] T118 [US4] Add logging for group operations

**Checkpoint**: P2 group management complete - Family plans and corporate accounts enabled

---

## Phase 8: User Story 5 - Notification Address Management (Priority: P3)

**Goal**: Enable operators to configure notification contact points (SMS, EMAIL) for subscribers or groups to receive service alerts, billing notifications, and account updates.

**Independent Test**: Create notification addresses for subscribers/groups, validate address format (msisdn for SMS, email for EMAIL), prevent duplicates, filter expired addresses.

### Tests for User Story 5

- [ ] T119 [P] [US5] Write integration test for POST /subscribers/{id}/notification-addresses (create notification) in src/test/java/com/telecom/ocs/provisioning/integration/NotificationAddressIntegrationTest.java
- [ ] T120 [P] [US5] Write integration test for GET /notification-addresses/{id} (retrieve notification) in src/test/java/com/telecom/ocs/provisioning/integration/NotificationAddressIntegrationTest.java
- [ ] T121 [P] [US5] Write integration test for address format validation (msisdn/email) in src/test/java/com/telecom/ocs/provisioning/integration/NotificationAddressIntegrationTest.java
- [ ] T122 [P] [US5] Write integration test for duplicate address prevention (409 Conflict) in src/test/java/com/telecom/ocs/provisioning/integration/NotificationAddressIntegrationTest.java
- [ ] T123 [P] [US5] Write integration test for expired address filtering in src/test/java/com/telecom/ocs/provisioning/integration/NotificationAddressIntegrationTest.java
- [ ] T124 [P] [US5] Write repository test for NotificationAddressRepository in src/test/java/com/telecom/ocs/provisioning/repository/NotificationAddressRepositoryTest.java
- [ ] T125 [P] [US5] Write unit test for NotificationAddressService with Mockito in src/test/java/com/telecom/ocs/provisioning/service/NotificationAddressServiceTest.java

### Implementation for User Story 5

- [ ] T126 [US5] Create NotificationAddress JPA entity with notificationType enum, subscriber/group FKs in src/main/java/com/telecom/ocs/provisioning/models/NotificationAddress.java
- [ ] T127 [US5] Create NotificationAddressRepository extending JpaRepository with custom queries in src/main/java/com/telecom/ocs/provisioning/repositories/NotificationAddressRepository.java
- [ ] T128 [US5] Implement NotificationAddressService with format validation, duplicate checking in src/main/java/com/telecom/ocs/provisioning/services/NotificationAddressService.java
- [ ] T129 [US5] Create NotificationAddressMapper for entity ↔ DTO conversion in src/main/java/com/telecom/ocs/provisioning/mappers/NotificationAddressMapper.java
- [ ] T130 [US5] Implement NotificationAddressController implementing generated NotificationAddressesApi interface in src/main/java/com/telecom/ocs/provisioning/controllers/NotificationAddressController.java
- [ ] T131 [US5] Add address format validation based on notificationType
- [ ] T132 [US5] Add duplicate address prevention logic
- [ ] T133 [US5] Add expired address filtering logic
- [ ] T134 [US5] Add logging for notification address operations

**Checkpoint**: P3 notification management complete - Customer communication channels enabled

---

## Phase 9: User Story 6 - Timer Scheduling (Priority: P3)

**Goal**: Enable operators to schedule time-based actions on entities (subscribers, subscriptions, groups) such as auto-renewal, expiration warnings, state transitions, or batch processing triggers.

**Independent Test**: Create timers linked to entities via timerEntityId, set execution dates (absolute or relative period), calculate absolute dates from relative periods, handle past-dated timers, verify cascade deletion.

### Tests for User Story 6

- [ ] T135 [P] [US6] Write integration test for POST /subscribers/{id}/timers (create timer) in src/test/java/com/telecom/ocs/provisioning/integration/TimerIntegrationTest.java
- [ ] T136 [P] [US6] Write integration test for GET /timers/{id} (retrieve timer) in src/test/java/com/telecom/ocs/provisioning/integration/TimerIntegrationTest.java
- [ ] T137 [P] [US6] Write integration test for relative period to absolute date calculation in src/test/java/com/telecom/ocs/provisioning/integration/TimerIntegrationTest.java
- [ ] T138 [P] [US6] Write integration test for past-dated timer handling (set to current timestamp) in src/test/java/com/telecom/ocs/provisioning/integration/TimerIntegrationTest.java
- [ ] T139 [P] [US6] Write integration test for timer cascade deletion on entity deletion in src/test/java/com/telecom/ocs/provisioning/integration/TimerIntegrationTest.java
- [ ] T140 [P] [US6] Write repository test for TimerRepository in src/test/java/com/telecom/ocs/provisioning/repository/TimerRepositoryTest.java
- [ ] T141 [P] [US6] Write unit test for TimerService with Mockito in src/test/java/com/telecom/ocs/provisioning/service/TimerServiceTest.java

### Implementation for User Story 6

- [ ] T142 [US6] Create Timer JPA entity with timerEntityId polymorphic FK, execution dates in src/main/java/com/telecom/ocs/provisioning/models/Timer.java
- [ ] T143 [US6] Create TimerRepository extending JpaRepository with findByTimerEntityId query in src/main/java/com/telecom/ocs/provisioning/repositories/TimerRepository.java
- [ ] T144 [US6] Implement TimerService with relative period calculation, past-date handling in src/main/java/com/telecom/ocs/provisioning/services/TimerService.java
- [ ] T145 [US6] Create TimerMapper for entity ↔ DTO conversion in src/main/java/com/telecom/ocs/provisioning/mappers/TimerMapper.java
- [ ] T146 [US6] Implement TimerController implementing generated TimersApi interface in src/main/java/com/telecom/ocs/provisioning/controllers/TimerController.java
- [ ] T147 [US6] Add relative period to absolute date calculation logic
- [ ] T148 [US6] Add past-dated timer handling (set to current timestamp)
- [ ] T149 [US6] Add cascade deletion configuration on entity relationships
- [ ] T150 [US6] Add logging for timer operations

**Checkpoint**: All P3 user stories complete - Full feature set implemented

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and final delivery artifacts

- [ ] T151 [P] Add optimistic locking validation in all service layer update operations (@Version field checks)
- [ ] T152 [P] Add correlation ID generation and MDC propagation in logging configuration
- [ ] T153 [P] Add pagination validation and metadata in all list endpoints
- [ ] T154 [P] Add PATCH operation field validation (FieldNotFound error for invalid fieldName)
- [ ] T155 [P] Add comprehensive JavaDoc comments to public APIs
- [ ] T156 Run Jacoco code coverage report and ensure 80% minimum coverage
- [ ] T157 Run integration tests with Testcontainers MySQL container
- [ ] T158 Validate OpenAPI spec compliance using Swagger UI
- [ ] T159 Test Docker container build and startup (verify <30s startup time)
- [ ] T160 Test docker-compose orchestration (REST service + MySQL)
- [ ] T161 Test deploy-schema.sh script on empty database
- [ ] T162 Test migrate-schema.sh script for schema fixes
- [ ] T163 Validate quickstart.md instructions end-to-end
- [ ] T164 Performance test: Verify <500ms p95 for subscriber/subscription operations
- [ ] T165 Performance test: Verify <200ms p95 for subscriber lookups
- [ ] T166 Load test: Verify 100 concurrent requests without degradation
- [ ] T167 [P] Update README.md with final build and deployment instructions
- [ ] T168 [P] Create API usage examples in quickstart.md
- [ ] T169 Tag release as v1.0.0 and push to repository

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (Phase 4)**: Depends on Foundational (Phase 2) + User Story 1 (Subscriber entity required)
- **User Story 3 (Phase 5)**: Depends on Foundational (Phase 2) + User Story 2 (Subscription entity required)
- **User Story 7 (Phase 6)**: Depends on Foundational (Phase 2) + User Story 1 (Subscriber entity required for history)
- **User Story 4 (Phase 7)**: Depends on Foundational (Phase 2) + User Story 1 (Subscriber entity required for group members)
- **User Story 5 (Phase 8)**: Depends on Foundational (Phase 2) + User Story 1 + User Story 4 (Subscriber and Group entities required)
- **User Story 6 (Phase 9)**: Depends on Foundational (Phase 2) + User Story 1-3 (entities for timer associations)
- **Polish (Phase 10)**: Depends on all desired user stories being complete

### Critical Path

1. Setup (Phase 1) → 17 tasks
2. Foundational (Phase 2) → 19 tasks (CRITICAL BLOCKER)
3. User Story 1 (Phase 3) → 16 tasks (MVP BASELINE)
4. User Story 2 (Phase 4) → 16 tasks (extends MVP)
5. User Story 3 (Phase 5) → 17 tasks (completes core P1 functionality)
6. Remaining phases can be scheduled based on priority

### Parallel Opportunities

- **Within Setup**: All tasks marked [P] (T003-T010, T012-T016) = 12 parallel tasks
- **Within Foundational**: Tasks T019-T020, T022-T025, T028-T029 = 8 parallel tasks
- **Within Each User Story**: All test tasks marked [P] can run in parallel
- **Across User Stories**: After Foundational phase, User Stories 7 and 4 can start in parallel with User Story 1
- **Within Polish**: Tasks T151-T155, T167-T168 = 6 parallel tasks

---

## Parallel Example: User Story 1

```bash
# Launch all test tasks for User Story 1 together:
T039: "Write integration test for POST /subscribers"
T040: "Write integration test for GET /subscribers/{id}"
T041: "Write integration test for GET /subscribers/lookup"
T042: "Write integration test for PATCH /subscribers/{id}"
T043: "Write integration test for DELETE /subscribers/{id}"
T044: "Write integration test for duplicate msisdn validation"
T045: "Write repository test for SubscriberRepository"
T046: "Write unit test for SubscriberService"
# All 8 test tasks can run in parallel (different files)
```

---

## Implementation Strategy

### MVP First (User Stories 1-3 Only)

1. Complete Phase 1: Setup (T001-T017) → ~1 day
2. Complete Phase 2: Foundational (T018-T038) → ~2 days (CRITICAL - includes validation)
3. Complete Phase 3: User Story 1 (T039-T054) → ~3 days
4. **VALIDATE MVP**: Test subscriber CRUD independently
5. Complete Phase 4: User Story 2 (T055-T070) → ~3 days
6. Complete Phase 5: User Story 3 (T071-T087) → ~3 days
7. **VALIDATE CORE**: Test subscriber + subscription + balance flow
8. **DEPLOY MVP**: Core P1 functionality ready (~12 days total)

### Incremental Delivery

- **Week 1**: Setup + Foundational + User Story 1 → Subscriber management live
- **Week 2**: User Story 2 + User Story 3 → Core charging system live (MVP!)
- **Week 3**: User Story 7 + User Story 4 → Audit + Groups live
- **Week 4**: User Story 5 + User Story 6 + Polish → Full feature set + production ready

### Parallel Team Strategy

With 3 developers after Foundational phase completes:

- **Developer A**: User Stories 1 → 2 → 3 (Core P1 path)
- **Developer B**: User Story 7 (Audit) → User Story 4 (Groups)
- **Developer C**: User Story 5 (Notifications) → User Story 6 (Timers)

---

## Task Summary

- **Total Tasks**: 169
- **Phase 1 (Setup)**: 17 tasks
- **Phase 2 (Foundational)**: 21 tasks (includes FR-076 and FR-079 validation)
- **Phase 3 (US1 - Subscriber)**: 16 tasks
- **Phase 4 (US2 - Subscription)**: 16 tasks
- **Phase 5 (US3 - Balance)**: 17 tasks
- **Phase 6 (US7 - AccountHistory)**: 14 tasks
- **Phase 7 (US4 - Group)**: 17 tasks
- **Phase 8 (US5 - NotificationAddress)**: 16 tasks
- **Phase 9 (US6 - Timer)**: 16 tasks
- **Phase 10 (Polish)**: 19 tasks

**Parallel Opportunities**: 54 tasks marked [P] across all phases

**MVP Scope**: Phases 1-5 (User Stories 1-3) = 87 tasks → ~12 days with TDD approach

**MVP Scope**: Phases 1-5 (User Stories 1-3) = 85 tasks → ~12 days with TDD approach

---

## Notes

- All tasks follow strict checklist format: `- [ ] [ID] [P?] [Story?] Description with file path`
- Tests written FIRST per constitution (Test-First Development principle)
- Each user story is independently testable and deployable
- [P] tasks work on different files with no dependencies
- Commit after completing each task or logical group
- Stop at checkpoints to validate story independently
- Constitution requirements: API-first (T030 OpenAPI gen), TDD (tests first), 80% coverage (T156), containerization (T032-T033), observability (T016, T036, T152)
