# Research & Decisions

## Overview
This document captures technology and architectural decisions for the OCS Provisioning Service. All items either reduce implementation risk or ensure alignment with constitution principles.

## Decisions

### Database: MySQL 8.0
- **Decision**: Use MySQL 8.0 with UTF8MB4 collation.
- **Rationale**: Widely supported, simple to containerize, strong tooling ecosystem.
- **Alternatives**: PostgreSQL (richer SQL features) rejected for simplicity and existing MySQL operational familiarity.

### Migrations: Flyway
- **Decision**: Versioned SQL migrations using Flyway.
- **Rationale**: Declarative, lightweight, integrates seamlessly with Maven/Spring Boot startup.
- **Alternatives**: Liquibase (more expressive XML/YAML formats) rejected for added complexity.

### Persistence Layer: Spring Data JPA + Hibernate
- **Decision**: Use Spring Data repositories and Hibernate as JPA provider.
- **Rationale**: Rapid CRUD development, pagination support, integration with Bean Validation.
- **Alternatives**: MyBatis rejected (manual SQL mapping overhead), jOOQ rejected (overkill for current CRUD scope).

### Optimistic Locking Strategy
- **Decision**: Implement via `@Version` field plus `lastModifiedDate` for client comparison (FR-072).
- **Rationale**: Prevent lost updates in concurrent modification scenarios with minimal overhead.
- **Alternatives**: Pessimistic locks rejected (higher contention, complexity).

### OpenAPI Code Generation
- **Decision**: Use OpenAPI Generator Maven plugin (server interface mode) to generate interfaces & DTOs.
- **Rationale**: Ensures contract-first compliance and reduces manual boilerplate.
- **Alternatives**: Manual controller definitions rejected (risk of drift from spec).

### Build & Containerization
- **Decision**: Multi-stage Dockerfile + Jib Maven plugin (optional). Final image uses Temurin JRE 17 slim.
- **Rationale**: Minimizes image size, faster builds, consistent Java runtime.
- **Alternatives**: Single-stage Dockerfile rejected (larger image), Distroless rejected (debugging complexity initially).

### Testing Strategy
- **Decision**: JUnit 5 + Mockito + Testcontainers for MySQL integration tests; H2 for repository focused tests when compatible.
- **Rationale**: Realistic integration environment while keeping unit tests fast.
- **Alternatives**: Pure H2 for all tests rejected (dialect differences), manual docker spin-up rejected (less automation).

### Logging & Observability
- **Decision**: Logback with JSON encoder, correlation IDs via MDC, Spring Boot Actuator endpoints.
- **Rationale**: Structured logs for ingestion, traceability, health/metrics out-of-the-box.
- **Alternatives**: Plain text logging rejected (lower machine parsing value).

### Apache Camel Usage
- **Decision**: Use Camel for future integration patterns (e.g., routing, timers ingestion) while core REST uses Spring MVC.
- **Rationale**: Extensible integration layer; separation of business logic from routing mechanics.
- **Alternatives**: Skip Camel initially rejected (anticipates upcoming integration tasks like batch processes or external triggers).

### Validation Approach
- **Decision**: Bean Validation annotations on DTOs; custom validators for msisdn pattern and expiration relations.
- **Rationale**: Declarative validation, centralized error handling.
- **Alternatives**: Manual validation code rejected (duplicated logic risk).

### Cascade Deletion Strategy
- **Decision**: Use JPA `@OnDelete(action=OnDeleteAction.CASCADE)` annotation for database-level cascade deletion on foreign key relationships.
- **Rationale**: Database-level cascade is more performant for large datasets than JPA `CascadeType.REMOVE`, which loads all dependent entities into memory before deletion. Database handles cascade atomically in single transaction.
- **Implementation**: Apply to Timer, Subscription, Balance, and NotificationAddress entities with foreign keys to Subscriber. Also applies to Balance → Subscription relationship.
- **Alternatives**: JPA `CascadeType.REMOVE` rejected (performance overhead for bulk deletes), manual deletion rejected (transaction complexity).

### Schema Deployment Scripts
- **Decision**: Shell scripts wrapping Flyway migration execution for deployment environments.
- **Rationale**: Simplified operator workflow; consistent DB state management.
- **Alternatives**: Manual SQL execution rejected (error prone).

## Open Items
No open NEEDS CLARIFICATION items identified in specification.

## Risks
| Risk | Impact | Mitigation |
|------|--------|-----------|
| High data volumes (tens of millions) | Query latency | Add indexes post initial profiling (subscriberId, msisdn, subscriptionId) |
| Rollover logic complexity | Incorrect balance transitions | Dedicated unit tests for rollover and expiration boundaries |
| Timer external execution dependency | Orphaned timers | Clear documentation and health checks for external scheduler ingestion |

## Summary
Decisions emphasize simplicity, contract-first delivery, and testability. All major selections align with constitution principles and project success criteria.
