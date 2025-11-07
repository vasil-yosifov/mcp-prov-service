<!--
Sync Impact Report:
- Version change: Initial → 1.0.0
- Modified principles: N/A (initial version)
- Added sections: All (initial constitution)
- Removed sections: None
- Templates requiring updates:
  ✅ .specify/templates/plan-template.md - reviewed, aligned with REST API and containerization principles
  ✅ .specify/templates/spec-template.md - reviewed, aligned with API-first design
  ✅ .specify/templates/tasks-template.md - reviewed, aligned with testing and observability requirements
- Follow-up TODOs: None
-->

# OCS Provisioning Service Constitution

## Core Principles

### I. API Contract First (NON-NEGOTIABLE)

OpenAPI specification MUST be defined and validated before implementation begins. All REST endpoints, request/response schemas, error codes, and status codes MUST be documented in the OpenAPI spec (`app-spec/ocs-provisioing-api.yml`). Implementation MUST conform exactly to the specification.

**Rationale**: Contract-first development ensures consistent API behavior, enables parallel development of clients and services, and provides living documentation that serves as the single source of truth.

### II. RESTful Design Standards (NON-NEGOTIABLE)

All API endpoints MUST follow REST principles:
- Use proper HTTP methods (GET for retrieval, POST for creation, PATCH for partial updates, DELETE for removal)
- Use appropriate HTTP status codes (200, 201, 204, 400, 404, 409, 422, 500)
- Resource-oriented URLs with hierarchical structure (e.g., `/subscribers/{subscriberId}/subscriptions`)
- Stateless operations
- Pagination MUST be supported via `limit` and `offset` query parameters for collection endpoints

**Rationale**: RESTful consistency reduces cognitive load, improves API predictability, and follows industry-standard best practices that clients expect.

### III. Test-First Development (NON-NEGOTIABLE)

Tests MUST be written before implementation code. Follow the Red-Green-Refactor cycle:
1. Write failing test (Red)
2. Implement minimum code to pass test (Green)
3. Refactor for quality while keeping tests green

All code MUST have:
- Unit tests using JUnit 5 for business logic
- Integration tests using `@SpringBootTest` for REST endpoints
- Repository tests using `@DataJpaTest` for data access layer
- Minimum 80% code coverage for all production code

**Rationale**: TDD ensures code correctness, prevents regressions, documents expected behavior, and creates maintainable, testable code architecture.

### IV. Containerization Standards

The service MUST be deployable as a Docker container with:
- Multi-stage Dockerfile for optimized image size
- Base image: OpenJDK 17 or later (preferably slim/alpine variants)
- Non-root user execution for security
- Health check endpoint (`/health-check`) for orchestration
- Externalized configuration via environment variables
- Container MUST start in under 30 seconds

**Rationale**: Containerization ensures consistent deployment across environments, simplifies orchestration, and supports cloud-native operations.

### V. Observability and Monitoring

All services MUST implement:
- Structured JSON logging using SLF4J with Logback
- Log levels: ERROR for failures, WARN for degraded states, INFO for business events, DEBUG for troubleshooting
- Spring Boot Actuator for health checks and metrics
- Request/response correlation IDs for distributed tracing
- No sensitive data (passwords, tokens, PII) in logs

**Rationale**: Comprehensive observability enables rapid troubleshooting, performance analysis, and proactive monitoring in production environments.

### VI. Data Validation and Error Handling

Input validation MUST be enforced at API boundaries:
- Use Bean Validation annotations (@Valid, @NotNull, @Pattern, etc.) on DTOs
- Return proper HTTP status codes with structured error responses
- Global exception handling via `@ControllerAdvice`
- Error responses MUST include: error code, human-readable message, and optional details
- Never expose stack traces or internal details to clients

**Rationale**: Explicit validation prevents invalid data from propagating through the system, provides clear feedback to API consumers, and improves system reliability.

## Technology Stack Requirements

### Mandatory Technologies
- **Language**: Java 17 or later
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven 3.9+
- **Database Access**: Spring Data JPA for relational databases
- **API Documentation**: Springdoc OpenAPI (auto-generated from code and OpenAPI spec)
- **Testing**: JUnit 5, MockMvc, Spring Boot Test
- **Container Runtime**: Docker

### Code Quality Standards
- Use Lombok for boilerplate reduction (mark as `provided` scope and `optional`)
- Constructor injection MUST be used over field injection
- Follow SOLID principles and maintain high cohesion, low coupling
- Package structure: `controllers`, `services`, `repositories`, `models`, `config`, `exceptions`
- Use Java 17+ features where applicable (records, sealed classes, pattern matching)

## Development Workflow

### Pre-Implementation Checklist
1. OpenAPI specification updated and reviewed
2. Data model documented in `app-spec/data-model.md`
3. Test cases defined and approved
4. Dependencies declared in `pom.xml` (minimal set only)

### Code Review Gates
All pull requests MUST:
- Include passing unit and integration tests
- Meet minimum 80% code coverage
- Have no unresolved linting/compilation errors
- Include updated API documentation if endpoints changed
- Follow naming conventions (PascalCase for classes, camelCase for methods/variables, ALL_CAPS for constants)
- Build successfully with `mvn clean verify`

### Deployment Requirements
- Service MUST pass health check before deployment
- Database migrations MUST be versioned and tested (use Flyway or Liquibase)
- Environment-specific configuration MUST use Spring Profiles (dev, test, prod)
- Container image MUST be tagged with semantic version

## Governance

This constitution supersedes all other development practices and MUST be followed by all contributors. Amendments require:
1. Documented justification for the change
2. Review and approval by project maintainers
3. Version increment following semantic versioning
4. Update to dependent templates and documentation

All code reviews MUST verify compliance with these principles. Violations MUST be addressed before merging. Complexity or deviations MUST be justified with explicit comments.

For runtime development guidance, refer to:
- `.github/instructions/java17-springboot.instructions.md`
- `.github/instructions/maven.instructions.md`

**Version**: 1.0.0 | **Ratified**: 2025-11-07 | **Last Amended**: 2025-11-07
