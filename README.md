# OCS Provisioning Service

Java 17 Spring Boot microservice for managing telecom subscribers, subscriptions, balances, groups, notification addresses, timers, and account history.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Data JPA (MySQL)
- Apache Camel
- Springdoc OpenAPI
- Testcontainers (MySQL)
- Docker / fabric8 docker-maven-plugin

## Quickstart

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker (for containerized runs and Testcontainers)
- Running MySQL instance or Dockerized MySQL

### Local Run (Dev Profile)

1. Set environment variables (optional, defaults are provided):
   - `DB_HOST` (default: `localhost`)
   - `DB_PORT` (default: `3306`)
   - `DB_NAME` (default: `ocs_provisioning`)
   - `DB_USER` (default: `ocsuser`)
   - `DB_PASSWORD` (default: `ocspass`)
2. Build and run tests:

```bash
mvn clean verify
```

3. Start the application:

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

### Profiles

- `dev` (default): local development with auto-update schema
- `test`: integration testing with dedicated schema
- `prod`: production settings with validated schema and externalized credentials

Activate a profile via `SPRING_PROFILES_ACTIVE`:

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

### API Documentation

Once running, OpenAPI/Swagger UI is available at:

- `http://localhost:8080/swagger-ui.html` (or `/swagger-ui/index.html` depending on Springdoc config)

### Docker

Build the application and Docker image automatically:

```bash
mvn clean package -DskipTests
```

The fabric8 docker-maven-plugin automatically builds the Docker image during the `package` phase. The image is tagged as:
`com.telecom.ocs.provisioning/ocs-provisioning-service:0.0.1-SNAPSHOT`

Run the container:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=ocs_provisioning_dev \
  -e DB_USER=ocsuser \
  -e DB_PASSWORD=ocspass \
  com.telecom.ocs.provisioning/ocs-provisioning-service:0.0.1-SNAPSHOT
```

Alternatively, use docker-compose for a complete environment (MySQL + App):

```bash
docker-compose up -d
```

This starts both the MySQL database and the application with preconfigured settings.

For more detailed scenarios, see `specs/001-ocs-provisioning-service/quickstart.md`.
