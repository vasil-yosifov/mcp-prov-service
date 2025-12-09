# Feature Specification: OCS Provisioning Service

**Feature Branch**: `001-ocs-provisioning-service`  
**Created**: 2025-11-07  
**Status**: Draft  
**Input**: User description: "Build standalone REST microservice for telecom subscriber profile management in online charging system with CRUD operations on subscribers, subscriptions, groups, balances, notifications, timers, and account history"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Subscriber Profile Management (Priority: P1)

Telecom operators need to create, retrieve, update, and delete subscriber profiles to manage customer accounts in the online charging system. A subscriber represents a telecom customer with personal information, billing details, service entitlements, and lifecycle state.

**Why this priority**: Subscriber management is the foundation of the provisioning system. Without subscribers, no other entities (subscriptions, balances, groups) can exist. This is the core business capability.

**Independent Test**: Can be fully tested by creating a new subscriber with personal info and services, retrieving it by subscriberId or lookup criteria (msisdn, imsi, name), updating subscriber state transitions, and deleting it. Delivers immediate value by enabling basic customer account provisioning.

**Acceptance Scenarios**:

1. **Given** no existing subscriber, **When** operator creates new subscriber with msisdn and personal info, **Then** system assigns unique subscriberId and returns complete subscriber profile with creation timestamp
2. **Given** existing subscriber in "active" state, **When** operator updates subscriber to "suspended" state, **Then** system records state transition with previousState and lastTransitionDate
3. **Given** existing subscriber with msisdn "43664123456789", **When** operator looks up subscriber by msisdn, **Then** system returns matching subscriberId not in "deactivated" state
4. **Given** subscriber with firstName "John" and lastName "Doe", **When** operator looks up by name, **Then** system returns matching subscriberId
5. **Given** existing subscriber, **When** operator deletes subscriber, **Then** system removes subscriber and all dependent entities (subscriptions, balances, timers)
6. **Given** duplicate msisdn and subscriber not in "deactivated", **When** operator attempts to create subscriber, **Then** system rejects with 409 Conflict error
7. **Given** duplicate subscriberId, **When** operator attempts to create subscriber, **Then** system rejects with 409 Conflict error

---

### User Story 2 - Subscription Lifecycle Management (Priority: P1)

Operators need to provision and manage service subscriptions for subscribers, including offers, recurring billing cycles, and subscription states. Each subscription represents a service package assigned to a subscriber.

**Why this priority**: Subscriptions are critical for service delivery and billing. They define what services a subscriber can use and must be operational for the charging system to function.

**Independent Test**: Can be fully tested by creating subscriptions under a subscriber, activating/suspending them, managing recurring cycles, and associating balances. Delivers value by enabling service provisioning and lifecycle management.

**Acceptance Scenarios**:

1. **Given** active subscriber, **When** operator creates new subscription with offerId and recurring settings, **Then** system assigns subscriptionId and initializes state as "pending"
2. **Given** pending subscription, **When** operator activates subscription, **Then** system sets state to "active" and records activationDate
3. **Given** active subscription with recurring=true and maxRecurringCycles=12, **When** subscription reaches cycle 12, **Then** system marks subscription state as "expired"
4. **Given** subscription with recurring cycles, **When** operator retrieves subscription, **Then** system shows recurringCyclesCompleted count
5. **Given** multiple subscriptions for one subscriber, **When** operator lists subscriptions for subscriberId, **Then** system returns all associated subscriptions
6. **Given** subscription in "active" state, **When** operator cancels subscription, **Then** system transitions to "cancelled" state
7. **Given** subscription for one subscriber, **When** operator deletes subscription, **Then** system removes the subscription and all dependent balances


---

### User Story 3 - Balance Tracking and Rollover (Priority: P1)

Operators need to track usage balances (data allowances, voice minutes, monetary credits) for subscriptions with proper expiration, rollover handling, and recurring cycle management.

**Why this priority**: Balances are essential for the online charging system to deduct usage and enforce service limits. Without balance tracking, charging cannot function.

**Independent Test**: Can be fully tested by creating balances under subscriptions, tracking available amounts vs consumed amounts, handling rollover at cycle boundaries, and enforcing expiration dates. Delivers value by enabling usage tracking and quota enforcement.

**Acceptance Scenarios**:

1. **Given** active subscription, **When** operator provisions balance with 5GB data allowance, **Then** system creates balance with balanceAmount=5368709120 bytes and balanceAvailable=5368709120
2. **Given** balance with unitType=BYTES, **When** usage is deducted, **Then** system decreases balanceAvailable while preserving balanceAmount
3. **Given** recurring balance with isRolloverAllowed=true and rolloverAmount=1500, **When** cycle completes with remaining balance, **Then** system rolls over up to maxRolloverAmount into next cycle
4. **Given** balance with expirationDate past current date, **When** operator retrieves balance, **Then** system marks balance as expired
5. **Given** subscription with multiple balances (data, voice, SMS), **When** operator lists balances for subscriptionId, **Then** system returns all balances with current available amounts
6. **Given** group balance with isGroupBalance=true, **When** group member consumes from balance, **Then** system deducts from shared group balance

---

### User Story 4 - Group Management (Priority: P2)

Operators need to create subscriber groups for family plans, corporate accounts, or shared subscriptions where multiple subscribers share quotas and services under a group owner.

**Why this priority**: Group functionality enables valuable use cases like family plans and corporate accounts, but the system can function for individual subscribers without it. This is an enhancement rather than core requirement.

**Independent Test**: Can be fully tested by creating groups with a group owner, adding/removing members, managing group subscriptions, and sharing balances across members. Delivers value by enabling multi-subscriber service packages.

**Acceptance Scenarios**:

1. **Given** existing subscriber, **When** operator creates group with subscriber as groupOwner, **Then** system assigns groupId and sets groupOwner.role to "ADMIN"
2. **Given** existing group, **When** operator adds member subscriberId to group, **Then** system adds member to members array and records memberSince timestamp
3. **Given** group with settings.maxMembers=5, **When** operator attempts to add 6th member, **Then** system rejects with error indicating max capacity reached
4. **Given** group with shared subscription, **When** operator assigns subscription to group, **Then** all group members can access subscription services
5. **Given** group owner, **When** operator removes group owner from group, **Then** system prevents removal or requires designation of new owner
6. **Given** active group, **When** operator terminates group, **Then** system sets state to "terminated" and unlinks all members

---

### User Story 5 - Notification Address Management (Priority: P3)

Operators need to configure notification contact points (SMS, EMAIL) for subscribers or groups to receive service alerts, billing notifications, and account updates.

**Why this priority**: Notifications enhance customer experience but are not critical for core provisioning and charging functionality. The system can operate without sending notifications.

**Independent Test**: Can be fully tested by creating notification addresses for subscribers/groups, validating address format (msisdn or email), and managing notification preferences. Delivers value by enabling customer communication channels.

**Acceptance Scenarios**:

1. **Given** existing subscriber, **When** operator creates notification address with notificationType=SMS and valid msisdn, **Then** system creates notificationAddressId and associates with subscriber
2. **Given** notification address with notificationType=EMAIL, **When** operator provides invalid email format, **Then** system rejects with validation error
3. **Given** duplicate notification address for subscriber, **When** operator attempts to create same address, **Then** system rejects with 409 Conflict
4. **Given** group notification address, **When** group receives notification, **Then** all members with that notification type receive the message
5. **Given** notification address with expirationDate, **When** date passes, **Then** system stops using address for notifications

---

### User Story 6 - Timer Scheduling (Priority: P3)

Operators need to schedule time-based actions on entities (subscribers, subscriptions, groups) such as auto-renewal, expiration warnings, state transitions, or batch processing triggers.

**Why this priority**: Timers enable automation and scheduled operations but are not required for basic provisioning. Manual operations can substitute initially.

**Independent Test**: Can be fully tested by creating timers linked to entities via timerEntityId, setting execution dates (absolute or relative period), and triggering timer execution. Delivers value by enabling automated workflows.

**Acceptance Scenarios**:

1. **Given** active subscription, **When** operator creates timer with timerExecutionDate for renewal reminder, **Then** system schedules timer and associates with subscriptionId via timerEntityId
2. **Given** timer with timerExecutionRelativePeriod=30 days, **When** timer is created, **Then** system calculates absolute timerExecutionDate from current date
3. **Given** subscriber entity, **When** operator creates timer for state transition at specific date, **Then** system links timer to subscriberId and executes action at scheduled time
4. **Given** expired timer (past executionDate), **When** operator retrieves timer, **Then** system indicates timer has executed or expired
5. **Given** timer associated with deleted entity, **When** entity is deleted, **Then** system cascades deletion to associated timers

---

### User Story 7 - Account History Tracking (Priority: P2)

Operators need to track all interactions and events on subscriber accounts, groups, and related entities for audit trails, compliance, and customer service inquiries.

**Why this priority**: Audit logging is important for compliance and troubleshooting but doesn't block core provisioning operations. Can be added after basic CRUD operations are working.

**Independent Test**: Can be fully tested by recording interactions on entities with timestamps, querying history by entityId, and retrieving specific interactions by interactionId. Delivers value by providing audit trail and compliance support.

**Acceptance Scenarios**:

1. **Given** subscriber state change, **When** system processes update, **Then** system creates accountHistory entry with entityType=SUBSCRIBER, entityId=subscriberId, description of change, and timestamp
2. **Given** multiple interactions on subscriber, **When** operator retrieves account history for entityId, **Then** system returns chronologically ordered list of all interactions
3. **Given** interaction with attachment (document, evidence), **When** operator creates history entry, **Then** system stores attachment metadata (id, url, type)
4. **Given** account history entry, **When** operator queries by interactionId, **Then** system returns detailed interaction record with all fields
5. **Given** account history for entityType=GROUP, **When** group membership changes, **Then** system records interaction with member details in description

---

### Edge Cases

- What happens when subscriber is deleted but has active subscriptions? **System must cascade delete or reject deletion with error indicating dependent entities exist**
- What happens when subscription reaches maxRecurringCycles? **System automatically transitions subscription to "expired" state and stops renewal**
- What happens when balance rolloverAmount exceeds maxRolloverAmount? **System caps rollover at maxRolloverAmount and discards excess**
- What happens when lookup query matches multiple subscribers? **System returns 400 Bad Request indicating ambiguous query, requiring more specific criteria**
- What happens when notification address expires but is still referenced? **System filters out expired addresses when sending notifications**
- What happens when timer execution date is in the past at creation? **System sets the timerExecutionDate to the timestamp of the provisioning request (current timestamp), allowing the timer to be scheduled from the moment of creation rather than rejecting or executing immediately**
- What happens when PATCH operation specifies invalid fieldName? **System returns 422 Unprocessable Entity with FieldNotFound error**
- What happens when pagination offset exceeds total record count? **System returns empty array with proper pagination metadata**
- What happens when concurrent updates modify the same entity? **System uses optimistic locking based on lastModifiedDate field: on update, system checks if lastModifiedDate matches the value from when client retrieved entity; if changed, returns 409 Conflict error requiring client to retry with fresh data**

## Requirements *(mandatory)*

### Functional Requirements

#### Subscriber Management (P1)

- **FR-001**: System MUST provide endpoint to create subscriber with required fields: msisdn (14-15 digit international format), and optional fields: personal info, billing address, services, custom fields
- **FR-002**: System MUST assign unique subscriberId automatically upon subscriber creation
- **FR-003**: System MUST support subscriber lifecycle states: pre-provisioned, active, suspended, deactivated, terminated
- **FR-004**: System MUST record previousState and lastTransitionDate on every state transition
- **FR-005**: System MUST provide lookup endpoint to find subscriberId by msisdn, imsi, or firstName+lastName combination
- **FR-006**: System MUST validate msisdn format as 14-15 digits matching pattern `^[0-9]{14,15}$`
- **FR-007**: System MUST prevent duplicate subscribers with same msisdn by returning 409 Conflict error
- **FR-008**: System MUST support partial update (PATCH) of subscriber fields via PatchField array format
- **FR-009**: System MUST support deletion of subscriber, handling cascade deletion of dependent entities or rejecting if dependencies exist
- **FR-010**: System MUST store subscriber creation timestamp in creationDate field

#### Subscription Management (P1)

- **FR-011**: System MUST provide endpoint to create subscription under specific subscriberId
- **FR-012**: System MUST assign unique subscriptionId automatically upon creation
- **FR-013**: System MUST support subscription states: pending, active, suspended, cancelled, expired
- **FR-014**: System MUST track recurring subscription cycles with maxRecurringCycles and recurringCyclesCompleted counters
- **FR-015**: System MUST automatically transition subscription to "expired" when recurringCyclesCompleted equals maxRecurringCycles
- **FR-016**: System MUST support cycleLengthType values: days, months, years for recurring cycles
- **FR-017**: System MUST calculate next renewalDate based on cycleLengthType and cycleLengthUnits
- **FR-018**: System MUST list subscriptions for a specific subscriberId with pagination (limit/offset)
- **FR-019**: System MUST prevent duplicate subscriptions with same subscriptionId by returning 409 Conflict
- **FR-020**: System MUST support retrieval of single subscription by subscriptionId

#### Balance Management (P1)

- **FR-021**: System MUST provide endpoint to create balance under specific subscriptionId
- **FR-022**: System MUST support balanceType values: ALLOWANCE, COUNTER
- **FR-023**: System MUST support unitType values: BYTES, SECONDS, EVENTS, MICROCENTS, MICROUNITS
- **FR-024**: System MUST track both balanceAmount (initial/total) and balanceAvailable (remaining) separately
- **FR-025**: System MUST enforce balance expiration by comparing expirationDate with current date
- **FR-026**: System MUST support balance rollover with isRolloverAllowed flag, rolloverAmount, and maxRolloverAmount
- **FR-027**: System MUST handle recurring balances with isRecurring flag and cycle tracking
- **FR-028**: System MUST support group balances with isGroupBalance flag allowing shared consumption
- **FR-029**: System MUST list balances for specific subscriptionId
- **FR-030**: System MUST cap rollover amount at maxRolloverAmount when cycle completes

#### Group Management (P2)

- **FR-031**: System MUST provide endpoint to create group with groupOwner designation
- **FR-032**: System MUST assign unique groupId automatically upon creation
- **FR-033**: System MUST support group states: active, suspended, terminated
- **FR-034**: System MUST track group members array with subscriberIds
- **FR-035**: System MUST enforce maxMembers limit from settings when adding members
- **FR-036**: System MUST prevent duplicate groupName by returning 409 Conflict
- **FR-037**: System MUST record memberSince timestamp when adding member to group
- **FR-038**: System MUST designate groupOwner with role (ADMIN or MEMBER)
- **FR-039**: System MUST provide endpoints to add/remove members from group
- **FR-040**: System MUST associate subscriptions and notification addresses with groups

#### Notification Address Management (P3)

- **FR-041**: System MUST provide endpoint to create notification address for subscriber or group
- **FR-042**: System MUST support notificationType values: SMS, EMAIL
- **FR-043**: System MUST validate notification address format based on type (msisdn for SMS, email format for EMAIL)
- **FR-044**: System MUST prevent duplicate notification addresses for same subscriber/group
- **FR-045**: System MUST track lastNotificationDate when notification is sent
- **FR-046**: System MUST filter out expired notification addresses (past expirationDate) when querying

#### Timer Management (P3)

- **FR-047**: System MUST provide endpoint to create timer associated with entity via timerEntityId
- **FR-048**: System MUST support both absolute timerExecutionDate and relative timerExecutionRelativePeriod
- **FR-049**: System MUST calculate absolute execution date from relative period at timer creation
- **FR-050**: System MUST associate timers with subscribers, subscriptions, or groups via scoped endpoints
- **FR-051**: System MUST cascade delete timers when associated entity is deleted
- **FR-052**: System MUST track timer creation, modification, and expiration timestamps
- **FR-053**: System MUST set timerExecutionDate to current request timestamp when provided timerExecutionDate is in the past

#### Account History Management (P2)

- **FR-054**: System MUST provide endpoint to record account history interactions
- **FR-055**: System MUST assign unique interactionId for each history entry
- **FR-056**: System MUST support entityType values: SUBSCRIBER, GROUP, ACCOUNT
- **FR-057**: System MUST associate history entries with specific entityId
- **FR-058**: System MUST list account history entries for specific entityId with pagination
- **FR-059**: System MUST retrieve specific history entry by interactionId
- **FR-060**: System MUST store interaction description, status, direction, reason, and timestamps
- **FR-061**: System MUST support attachment metadata (id, url, type) in history entries

#### API Standards (P1)

- **FR-062**: System MUST implement health check endpoint at `/health-check` returning 200 OK when healthy
- **FR-063**: System MUST support pagination with limit (1-100) and offset (≥0) query parameters for list endpoints
- **FR-064**: System MUST return proper HTTP status codes per RFC 7231 status code semantics: 200 OK, 201 Created, 204 No Content, 400 Bad Request, 404 Not Found, 409 Conflict, 422 Unprocessable Entity, 500 Internal Server Error
- **FR-065**: System MUST return structured error responses with code, message, and optional details object
- **FR-066**: System MUST support partial updates via PATCH with PatchField array containing fieldName and fieldValue
- **FR-067**: System MUST return 422 Unprocessable Entity with FieldNotFound error when PATCH specifies invalid fieldName
- **FR-068**: System MUST conform exactly to OpenAPI specification in `app-spec/ocs-provisioing-api.yml`
- **FR-069**: System MUST timestamp all entity creation with creationDate field
- **FR-070**: System MUST timestamp all entity modifications with lastModifiedDate field
- **FR-071**: System MUST serve API at base path `/ocs/prov/v1`
- **FR-072**: System MUST implement optimistic locking for entity updates by validating lastModifiedDate on PATCH/PUT operations and returning 409 Conflict if entity was modified since client retrieval. Client MUST refresh entity and reapply changes upon receiving 409 Conflict response.

#### Deployment & Infrastructure (P1)

- **FR-073**: System MUST be packaged as Docker container image
- **FR-074**: System MUST provide Dockerfile using multi-stage build for optimized image size
- **FR-075**: System MUST provide docker-compose.yml orchestrating REST service container and RDBMS container
- **FR-076**: System MUST connect to external RDBMS for persistent storage
- **FR-077**: System MUST provide database schema deployment script that creates missing tables
- **FR-078**: System MUST provide database schema migration script that fixes schema inconsistencies
- **FR-079**: System MUST externalize configuration via environment variables (database connection, ports, etc.)
- **FR-080**: System MUST start within 30 seconds in containerized environment

### Key Entities

- **Subscriber**: Represents a telecom customer with personal info (name, DOB, email, contact), billing details (billing cycle, address), service entitlements (voice, SMS, data, roaming, VAS), lifecycle state (pre-provisioned, active, suspended, deactivated, terminated), and relationships to subscriptions, groups, notifications, timers. Uniquely identified by subscriberId.

- **Subscription**: Represents a service package provisioned for a subscriber with offer details (offerId, offerName), lifecycle state (pending, active, suspended, cancelled, expired), recurring billing cycle configuration (recurring flag, maxRecurringCycles, cycleLengthType/Units), and associations to balances and timers. Uniquely identified by subscriptionId, belongs to one subscriber via subscriberId.

- **Balance**: Represents usage quota or allowance for a subscription with balance type (ALLOWANCE, COUNTER), unit type (BYTES, SECONDS, EVENTS, MICROCENTS, MICROUNITS), amounts (balanceAmount for total, balanceAvailable for remaining), expiration/effective dates, rollover settings (isRolloverAllowed, rolloverAmount, maxRolloverAmount), and recurring cycle tracking. Can be individual or group-shared (isGroupBalance flag). Belongs to one subscription via subscriptionId.

- **Group**: Represents a collection of subscribers with shared services/subscriptions for family plans or corporate accounts. Has group owner (subscriberId with ADMIN role), members array (subscriberIds), group state (active, suspended, terminated), settings (maxMembers, autoRenew, quotaDistribution), and associations to subscriptions, notifications, timers. Uniquely identified by groupId.

- **NotificationAddress**: Represents a contact endpoint for notifications with type (SMS, EMAIL), address value (msisdn or email), timestamps (lastNotificationDate, createdDate, expirationDate). Associated with subscriber or group. Uniquely identified by notificationAddressId.

- **Timer**: Represents a scheduled action on an entity with execution timing (absolute timerExecutionDate or relative timerExecutionRelativePeriod), entity association (timerEntityId references subscriber/subscription/group), timer name, and timestamps (createdDate, modifiedDate, expirationDate). Uniquely identified by timerId.

- **AccountHistory**: Represents an audit log entry for entity interactions with interaction details (description, status, direction, reason), entity reference (entityId, entityType: SUBSCRIBER/GROUP/ACCOUNT), optional attachment metadata (id, url, type), interaction date range (startDateTime, endDateTime), and channel. Uniquely identified by interactionId.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Operators can create a new subscriber profile with personal info and services in under 30 seconds via API
- **SC-002**: System handles 100 concurrent API requests for subscriber/subscription operations without response time exceeding 500ms for 95th percentile
- **SC-003**: Operators can look up subscriber by msisdn, imsi, or name and retrieve profile within 200ms for 95% of requests
- **SC-004**: System successfully processes 1000 balance deduction operations per second with accurate balance tracking
- **SC-005**: Docker container starts successfully and reaches healthy state within 30 seconds
- **SC-006**: Database schema deployment script creates all required tables (subscribers, subscriptions, balances, groups, notificationAddresses, timers, accountHistory) from empty database in under 60 seconds
- **SC-007**: System maintains 99.9% uptime when deployed in containerized environment with proper health checks
- **SC-008**: All API endpoints return proper HTTP status codes and structured error messages with 100% conformance to OpenAPI specification
- **SC-009**: Operators can provision complete subscriber account (subscriber + subscription + balance) in under 5 API calls with zero data loss
- **SC-010**: System persists all entity state changes to RDBMS with ACID guarantees ensuring no data corruption
- **SC-011**: Account history audit trail captures 100% of entity state transitions with accurate timestamps
- **SC-012**: Group balance sharing correctly deducts from shared balance across all group members with no double-counting
- **SC-013**: Recurring subscription cycles automatically transition state at cycle boundaries with 100% accuracy
- **SC-014**: System prevents duplicate entity creation (same msisdn, subscriptionId, etc.) with 100% consistency returning 409 Conflict
- **SC-015**: Pagination for list endpoints correctly handles offset/limit parameters returning consistent results across pages

## Assumptions

- RDBMS will be MySQL 8.0+ relational database accessible via JDBC (decision ratified in research.md for UTF8MB4 support, team familiarity, and production stability)
- Database connection details (host, port, credentials, schema name) provided via environment variables
- System will run in single-instance deployment initially (no clustering/HA requirements)
- API consumers are authenticated and authorized external systems (authentication/authorization handled by API gateway upstream)
- Clock synchronization across containers is handled by container orchestration platform
- Network connectivity between REST service container and RDBMS container is reliable with <10ms latency
- RDBMS has sufficient storage for expected data volumes (assumption: millions of subscribers, tens of millions of subscriptions/balances)
- Backup and disaster recovery for RDBMS is handled externally by infrastructure team
- Monitoring and alerting infrastructure exists to consume health check endpoint
- Timer execution mechanism (scheduler/cron) is provided by external system consuming timer entity data
- Actual notification sending (SMS/email delivery) is handled by external notification service
- Balance deduction operations are triggered by external charging system consuming balance APIs
- API versioning strategy follows major.minor semantic versioning reflected in URL path (`/v1`)

## Out of Scope

- User authentication and authorization (handled by upstream API gateway)
- Encryption of data at rest or in transit (TLS termination at load balancer)
- Actual execution of scheduled timers (timer entities store schedule, external scheduler executes)
- Sending actual SMS/email notifications (system only stores notification addresses)
- Real-time balance deduction logic (external charging system deducts, this system tracks)
- Multi-region deployment or geographical data residency
- High availability clustering or failover mechanisms
- Performance tuning beyond basic indexing
- Backup and restore procedures for database
- Monitoring dashboards or alerting rules
- API rate limiting or throttling (handled by API gateway)
- WebSocket or streaming APIs for real-time updates
- Bulk import/export of entities
- Advanced search or filtering beyond basic lookup
- Soft delete or data archival strategies
- Internationalization or localization of error messages
- API documentation UI (OpenAPI spec provided, UI hosting separate)
