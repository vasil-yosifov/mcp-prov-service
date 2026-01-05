# Data Model

## Overview
Entity definitions extracted from feature specification and OpenAPI schema. All entities mapped to JPA with Hibernate as provider, persisted to MySQL 8.0.

---

## Entities

### Subscriber
**Description**: Represents a telecom customer account in the online charging system.

**Primary Key**: `subscriberId` (String, UUID)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| subscriberId | String | PK, NOT NULL, UUID | Unique subscriber identifier |
| msisdn | String | NOT NULL, UNIQUE (if not deactivated), Pattern: `^[0-9]{14,15}$` | International phone number (14-15 digits) |
| imsi | String | NULLABLE, Length: 15 | International Mobile Subscriber Identity |
| firstName | String | NULLABLE | Subscriber first name |
| lastName | String | NULLABLE | Subscriber last name |
| dateOfBirth | LocalDate | NULLABLE | Subscriber date of birth |
| email | String | NULLABLE, Email format | Contact email |
| contactNumber | String | NULLABLE | Additional contact number |
| billingCycle | Integer | NULLABLE | Billing cycle day (1-31) |
| billingAddress | String | NULLABLE | Billing address text |
| services | JSON/String | NULLABLE | Service entitlements (voice, SMS, data, roaming, VAS) |
| state | Enum | NOT NULL, Default: PRE_PROVISIONED | Lifecycle state |
| previousState | Enum | NULLABLE | Previous lifecycle state |
| lastTransitionDate | Timestamp | NULLABLE | Date of last state transition |
| creationDate | Timestamp | NOT NULL | Subscriber creation timestamp |
| lastModifiedDate | Timestamp | NOT NULL | Last modification timestamp |
| version | Long | @Version | Optimistic locking version |

**State Values**: `PRE_PROVISIONED`, `ACTIVE`, `SUSPENDED`, `DEACTIVATED`, `TERMINATED`

**Relationships**:
- One-to-Many: `subscriptions` → Subscription (cascade DELETE)
- Many-to-Many: `groups` → Group (via membership)
- One-to-Many: `notificationAddresses` → NotificationAddress (cascade DELETE)
- One-to-Many: `timers` → Timer (cascade DELETE)

**Validation Rules**:
- FR-006: msisdn matches pattern `^[0-9]{14,15}$`
- FR-007: msisdn unique constraint (exclude deactivated state)
- FR-004: previousState and lastTransitionDate updated on state change

**Indexes**:
- `idx_subscriber_msisdn` on msisdn
- `idx_subscriber_imsi` on imsi
- `idx_subscriber_name` on (firstName, lastName)
- `idx_subscriber_state` on state

---

### Subscription
**Description**: Represents a service package provisioned for a subscriber.

**Primary Key**: `subscriptionId` (String, UUID)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| subscriptionId | String | PK, NOT NULL, UUID | Unique subscription identifier |
| subscriberId | String | FK, NOT NULL | Reference to parent subscriber |
| offerId | String | NOT NULL | Offer identifier |
| offerName | String | NULLABLE | Offer display name |
| state | Enum | NOT NULL, Default: PENDING | Subscription lifecycle state |
| activationDate | Timestamp | NULLABLE | Date subscription activated |
| recurring | Boolean | NOT NULL, Default: false | Whether subscription recurs |
| maxRecurringCycles | Integer | NULLABLE | Maximum recurring cycles (if recurring=true) |
| recurringCyclesCompleted | Integer | NOT NULL, Default: 0 | Count of completed cycles |
| cycleLengthType | Enum | NULLABLE | Cycle duration unit (days/months/years) |
| cycleLengthUnits | Integer | NULLABLE | Cycle duration value |
| renewalDate | Timestamp | NULLABLE | Next renewal date |
| creationDate | Timestamp | NOT NULL | Subscription creation timestamp |
| lastModifiedDate | Timestamp | NOT NULL | Last modification timestamp |
| version | Long | @Version | Optimistic locking version |

**State Values**: `PENDING`, `ACTIVE`, `SUSPENDED`, `CANCELLED`, `EXPIRED`

**Relationships**:
- Many-to-One: `subscriber` → Subscriber (FK subscriberId)
- One-to-Many: `balances` → Balance (cascade DELETE)
- One-to-Many: `timers` → Timer (cascade DELETE)

**Validation Rules**:
- FR-015: Auto-transition to EXPIRED when recurringCyclesCompleted == maxRecurringCycles
- FR-017: Calculate renewalDate based on cycleLengthType and cycleLengthUnits
- FR-019: Unique subscriptionId constraint

**Indexes**:
- `idx_subscription_subscriber` on subscriberId
- `idx_subscription_state` on state

---

### Balance
**Description**: Represents usage quota or allowance for a subscription.

**Primary Key**: `balanceId` (String, UUID)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| balanceId | String | PK, NOT NULL, UUID | Unique balance identifier |
| subscriptionId | String | FK, NOT NULL | Reference to parent subscription |
| balanceType | Enum | NOT NULL | Balance type (ALLOWANCE/COUNTER) |
| unitType | Enum | NOT NULL | Unit type (BYTES/SECONDS/EVENTS/MICROCENTS/MICROUNITS) |
| balanceAmount | Long | NOT NULL | Initial/total balance amount |
| balanceAvailable | Long | NOT NULL | Remaining balance amount |
| expirationDate | Timestamp | NULLABLE | Balance expiration date |
| effectiveDate | Timestamp | NULLABLE | Balance effective start date |
| isRolloverAllowed | Boolean | NOT NULL, Default: false | Whether rollover is allowed |
| rolloverAmount | Long | NULLABLE | Amount to rollover |
| maxRolloverAmount | Long | NULLABLE | Maximum rollover cap |
| isRecurring | Boolean | NOT NULL, Default: false | Whether balance recurs |
| isGroupBalance | Boolean | NOT NULL, Default: false | Whether shared across group |
| creationDate | Timestamp | NOT NULL | Balance creation timestamp |
| lastModifiedDate | Timestamp | NOT NULL | Last modification timestamp |
| version | Long | @Version | Optimistic locking version |

**Enum Values**:
- balanceType: `ALLOWANCE`, `COUNTER`
- unitType: `BYTES`, `SECONDS`, `EVENTS`, `MICROCENTS`, `MICROUNITS`

**Relationships**:
- Many-to-One: `subscription` → Subscription (FK subscriptionId)

**Validation Rules**:
- FR-025: Check expirationDate against current date to mark expired
- FR-030: Cap rolloverAmount at maxRolloverAmount when cycle completes
- FR-024: Track balanceAmount (total) and balanceAvailable (remaining) separately

**Indexes**:
- `idx_balance_subscription` on subscriptionId
- `idx_balance_expiration` on expirationDate

---

### Group
**Description**: Collection of subscribers for shared services (family plans, corporate accounts).

**Primary Key**: `groupId` (String, UUID)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| groupId | String | PK, NOT NULL, UUID | Unique group identifier |
| groupName | String | NOT NULL, UNIQUE | Group display name |
| groupOwnerId | String | FK, NOT NULL | Subscriber ID of group owner |
| groupOwnerRole | Enum | NOT NULL | Role of group owner (ADMIN/MEMBER) |
| state | Enum | NOT NULL, Default: ACTIVE | Group lifecycle state |
| maxMembers | Integer | NULLABLE | Maximum group members allowed |
| autoRenew | Boolean | NOT NULL, Default: false | Auto-renewal flag |
| quotaDistribution | String | NULLABLE | Quota distribution strategy |
| members | JSON/String | NULLABLE | Array of member subscriberIds with memberSince timestamps |
| creationDate | Timestamp | NOT NULL | Group creation timestamp |
| lastModifiedDate | Timestamp | NOT NULL | Last modification timestamp |
| version | Long | @Version | Optimistic locking version |

**State Values**: `ACTIVE`, `SUSPENDED`, `TERMINATED`

**Relationships**:
- Many-to-One: `groupOwner` → Subscriber (FK groupOwnerId)
- Many-to-Many: `members` → Subscriber (via JSON or join table)
- One-to-Many: `notificationAddresses` → NotificationAddress (cascade DELETE)
- One-to-Many: `timers` → Timer (cascade DELETE)

**Validation Rules**:
- FR-035: Enforce maxMembers limit when adding members
- FR-036: Unique groupName constraint
- FR-037: Record memberSince timestamp for each member
- FR-038: Prevent removing group owner without reassignment

**Indexes**:
- `idx_group_name` on groupName
- `idx_group_owner` on groupOwnerId
- `idx_group_state` on state

---

### NotificationAddress
**Description**: Contact endpoint for sending notifications to subscribers or groups.

**Primary Key**: `notificationAddressId` (String, UUID)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| notificationAddressId | String | PK, NOT NULL, UUID | Unique notification address identifier |
| subscriberId | String | FK, NULLABLE | Reference to subscriber (if subscriber-level) |
| groupId | String | FK, NULLABLE | Reference to group (if group-level) |
| notificationType | Enum | NOT NULL | Notification type (SMS/EMAIL) |
| address | String | NOT NULL | Address value (msisdn or email) |
| lastNotificationDate | Timestamp | NULLABLE | Last notification sent date |
| expirationDate | Timestamp | NULLABLE | Address expiration date |
| createdDate | Timestamp | NOT NULL | Address creation timestamp |
| lastModifiedDate | Timestamp | NOT NULL | Last modification timestamp |
| version | Long | @Version | Optimistic locking version |

**Enum Values**:
- notificationType: `SMS`, `EMAIL`

**Relationships**:
- Many-to-One: `subscriber` → Subscriber (FK subscriberId, nullable)
- Many-to-One: `group` → Group (FK groupId, nullable)

**Validation Rules**:
- FR-043: Validate address format based on notificationType (msisdn for SMS, email for EMAIL)
- FR-044: Prevent duplicate addresses for same subscriber/group
- FR-046: Filter expired addresses when querying

**Indexes**:
- `idx_notification_subscriber` on subscriberId
- `idx_notification_group` on groupId
- `idx_notification_expiration` on expirationDate

**Constraints**:
- Check: Either subscriberId OR groupId must be non-null (not both)

---

### Timer
**Description**: Scheduled action associated with an entity (subscriber, subscription, group).

**Primary Key**: `timerId` (String, UUID)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| timerId | String | PK, NOT NULL, UUID | Unique timer identifier |
| timerEntityId | String | NOT NULL | Entity ID (subscriberId/subscriptionId/groupId) |
| timerEntityType | String | NOT NULL | Entity type identifier |
| timerName | String | NULLABLE | Timer display name |
| timerExecutionDate | Timestamp | NOT NULL | Absolute execution date |
| timerExecutionRelativePeriod | String | NULLABLE | Relative period (e.g., "30 days") |
| createdDate | Timestamp | NOT NULL | Timer creation timestamp |
| modifiedDate | Timestamp | NULLABLE | Timer modification timestamp |
| expirationDate | Timestamp | NULLABLE | Timer expiration date |
| version | Long | @Version | Optimistic locking version |

**Relationships**:
- Many-to-One: `entity` (polymorphic via timerEntityId + timerEntityType)

**Validation Rules**:
- FR-049: Calculate absolute timerExecutionDate from relative period at creation
- FR-053: If provided timerExecutionDate is in past, set to current request timestamp
- FR-051: Cascade delete timers when associated entity deleted

**Indexes**:
- `idx_timer_entity` on (timerEntityId, timerEntityType)
- `idx_timer_execution` on timerExecutionDate

---

### AccountHistory
**Description**: Audit log entry for entity interactions and state changes.

**Primary Key**: `interactionId` (String, UUID)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| interactionId | String | PK, NOT NULL, UUID | Unique interaction identifier |
| entityId | String | NOT NULL | Entity ID (subscriberId/groupId) |
| entityType | Enum | NOT NULL | Entity type (SUBSCRIBER/GROUP/ACCOUNT) |
| description | String | NOT NULL | Interaction description |
| status | String | NULLABLE | Interaction status |
| direction | String | NULLABLE | Interaction direction |
| reason | String | NULLABLE | Interaction reason |
| channel | String | NULLABLE | Interaction channel |
| startDateTime | Timestamp | NOT NULL | Interaction start timestamp |
| endDateTime | Timestamp | NULLABLE | Interaction end timestamp |
| attachmentId | String | NULLABLE | Attachment identifier |
| attachmentUrl | String | NULLABLE | Attachment URL |
| attachmentType | String | NULLABLE | Attachment MIME type |
| creationDate | Timestamp | NOT NULL | History entry creation timestamp |

**Enum Values**:
- entityType: `SUBSCRIBER`, `GROUP`, `ACCOUNT`

**Relationships**:
- Many-to-One: `entity` (polymorphic via entityId + entityType)

**Validation Rules**:
- FR-058: Support pagination (limit/offset) when listing history for entityId
- FR-060: Store interaction details including status, direction, reason, timestamps

**Indexes**:
- `idx_history_entity` on (entityId, entityType)
- `idx_history_start` on startDateTime

---

### Usage
**Description**: Represents a service consumption record for a subscriber, tracking usage of voice, data, SMS, or MMS services and their impact on balances.

**Primary Key**: `usageId` (String, UUID)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| usageId | String | PK, NOT NULL, UUID | Unique usage record identifier |
| usageTimestamp | Timestamp | NOT NULL | Record creation timestamp |
| chargedPartyId | String | FK, NOT NULL | Reference to subscriber being charged |
| chargedMsisdn | String | NULLABLE, Pattern: `^[0-9]{11,15}$` | MSISDN being charged |
| aParty | String | NULLABLE | Originating MSISDN |
| bParty | String | NULLABLE | Terminating MSISDN or APN |
| usageType | Enum | NOT NULL | Type of usage (VOICE/DATA/SMS/MMS) |
| recordType | Enum | NOT NULL | Type of record (START/INTERIM/STOP/EVENT) |
| recordOpeningTime | Timestamp | NULLABLE | Session start timestamp |
| recordClosingTime | Timestamp | NULLABLE | Session end timestamp |
| durationSeconds | Integer | NULLABLE | Duration in seconds |
| volumeUsage | Long | NOT NULL | Usage volume (bytes/seconds/count) |
| impactedBalanceId | String | FK, NOT NULL | Reference to impacted balance |
| balanceValueBefore | Long | NULLABLE | Balance value before usage |
| balanceValueAfter | Long | NULLABLE | Balance value after usage |
| offerId | String | NULLABLE | Associated offer identifier |

**Enum Values**:
- usageType: `VOICE`, `DATA`, `SMS`, `MMS`
- recordType: `START`, `INTERIM`, `STOP`, `EVENT`

**Relationships**:
- Many-to-One: `subscriber` → Subscriber (FK chargedPartyId)
- Many-to-One: `balance` → Balance (FK impactedBalanceId)

**Validation Rules**:
- FR-091: Validate chargedPartyId exists (subscriber lookup)
- FR-092: Prevent duplicate usageId
- FR-087: volumeUsage interpretation depends on usageType (bytes for DATA, seconds for VOICE, count for SMS/MMS)

**Indexes**:
- `idx_usage_subscriber` on chargedPartyId
- `idx_usage_balance` on impactedBalanceId
- `idx_usage_timestamp` on usageTimestamp
- `idx_usage_type` on usageType

---

## Entity Relationship Diagram

```
Subscriber (1) ──────< (N) Subscription (1) ──────< (N) Balance (1) ──────< (N) Usage
    │                           │                           │
    │                           └──────< (N) Timer          │
    │                                                       │
    ├──────< (N) NotificationAddress                        │
    │                                                       │
    ├──────< (N) Timer                                      │
    │                                                       │
    ├──────< (N) AccountHistory                             │
    │                                                       │
    ├──────< (N) Usage (via chargedPartyId) ────────────────┘
    │
    └──────< (N) Group (owner)
                 │
                 └──────< (N) NotificationAddress
                 │
                 └──────< (N) Timer
```

## JPA Mapping Notes

- All entities use `@Entity` annotation
- Primary keys generated via `UUID.randomUUID()` or database strategy
- Timestamps use `@CreatedDate` and `@LastModifiedDate` with JPA auditing enabled
- Optimistic locking via `@Version` field (Long type) - Note: Usage entity does not use optimistic locking as records are immutable once created
- Enums mapped via `@Enumerated(EnumType.STRING)` for readability
- Foreign keys with `@ManyToOne` and `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`
- JSON fields stored as String with custom converters or use Hibernate JSON types
- All entities implement proper `equals()` and `hashCode()` based on ID

## Database Schema Migration

Flyway migrations stored in `src/main/resources/db/migration/`:
- `V1__initial_schema.sql` — Create all tables with constraints (8 entities: Subscriber, Subscription, Balance, Group, NotificationAddress, Timer, AccountHistory, Usage)
- `V2__add_indexes.sql` — Add performance indexes
- `V3__add_usage_table.sql` — Add usage table (if added incrementally)
- Future migrations numbered sequentially (V4__, V5__, etc.)

Migration executed automatically on Spring Boot startup or via deployment script.
