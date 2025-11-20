# Quick Start Guide

## Prerequisites

- Java 17 JDK (Temurin/OpenJDK recommended)
- Maven 3.9+
- Docker 20.10+
- Docker Compose 2.0+
- Git

## Initial Setup

### 1. Clone Repository

```bash
git clone <repository-url>
cd mcp-prov-service
git checkout 001-ocs-provisioning-service
```

### 2. Build Application

Build the application including OpenAPI code generation and tests:

```bash
mvn clean install
```

This will:
- Generate API interfaces and DTOs from `app-spec/ocs-provisioing-api.yml`
- Compile Java source code
- Run unit tests
- Run integration tests
- Package application as executable JAR
- Generate code coverage report (target/site/jacoco/index.html)

### 3. Run Tests Only

```bash
# Unit tests only
mvn test

# Integration tests only
mvn verify -DskipUTs

# All tests with coverage
mvn clean verify
```

## Running Locally

### Option 1: Docker Compose (Recommended)

Build the application and Docker image:

```bash
mvn clean package -DskipTests
```

Start both REST service and MySQL database:

```bash
docker-compose up -d
```

Service will be available at: `http://localhost:8080/ocs/prov/v1`

Health check: `http://localhost:8080/health-check`

Stop services:

```bash
docker-compose down
```

### Option 2: Local Java (MySQL via Docker)

Start MySQL only:

```bash
docker-compose up -d mysql
```

Run application locally:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or run packaged JAR:

```bash
java -jar target/ocs-provisioning-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Option 3: IDE (IntelliJ IDEA / Eclipse)

1. Import project as Maven project
2. Start MySQL via `docker-compose up -d mysql`
3. Run `OcsProvisioningApplication.java` with VM option: `-Dspring.profiles.active=dev`

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile (dev/test/prod) | `dev` |
| `DB_HOST` | MySQL host | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | Database name | `ocs_provisioning_dev` |
| `DB_USER` | Database username | `ocsuser` |
| `DB_PASSWORD` | Database password | `ocspass` |
| `SERVER_PORT` | REST service port | `8080` |
| `LOG_LEVEL` | Logging level | `INFO` |

### Profiles

- **dev**: Development with verbose logging, H2 console enabled
- **test**: Test profile with in-memory database
- **prod**: Production with optimized settings, JSON logging

## Database Management

### Initial Schema Deployment

Deploy database schema to empty database:

```bash
./scripts/deploy-schema.sh
```

This executes Flyway migrations from `src/main/resources/db/migration/`.

### Schema Migration

Fix schema inconsistencies or apply updates:

```bash
./scripts/migrate-schema.sh
```

### Manual SQL Access

Connect to MySQL container:

```bash
docker exec -it mysql mysql -u ocsuser -pocspass ocs_provisioning_dev
```

## API Usage

### Health Check

```bash
curl http://localhost:8080/health-check
```

Expected response: `200 OK`

### Create Subscriber

```bash
curl -X POST http://localhost:8080/ocs/prov/v1/subscribers \
  -H "Content-Type: application/json" \
  -d '{
    "msisdn": "43664123456789",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com"
  }'
```

### Lookup Subscriber by MSISDN

```bash
curl "http://localhost:8080/ocs/prov/v1/subscribers/lookup?msisdn=43664123456789"
```

### Get Subscriber by ID

```bash
curl http://localhost:8080/ocs/prov/v1/subscribers/{subscriberId}
```

### Update Subscriber (PATCH)

```bash
curl -X PATCH http://localhost:8080/ocs/prov/v1/subscribers/{subscriberId} \
  -H "Content-Type: application/json" \
  -d '[
    {
      "fieldName": "state",
      "fieldValue": "ACTIVE"
    }
  ]'
```

### Delete Subscriber

```bash
curl -X DELETE http://localhost:8080/ocs/prov/v1/subscribers/{subscriberId}
```

### API Documentation

Interactive API docs (Swagger UI):

```
http://localhost:8080/swagger-ui.html
```

OpenAPI specification JSON:

```
http://localhost:8080/v3/api-docs
```

## Troubleshooting

### Port Already in Use

Change `SERVER_PORT` environment variable:

```bash
export SERVER_PORT=8081
docker-compose up --build
```

### Database Connection Failed

Verify MySQL is running:

```bash
docker ps | grep mysql
```

Check MySQL logs:

```bash
docker logs mysql
```

### Application Won't Start

Check application logs:

```bash
docker logs ocs-provisioning-service
```

Verify Java version:

```bash
java -version  # Should be 17+
```

### Tests Failing

Run with verbose output:

```bash
mvn clean test -X
```

Check test reports:

```
target/surefire-reports/
target/failsafe-reports/
```

### Code Coverage Below 80%

View coverage report:

```bash
open target/site/jacoco/index.html
```

Identify uncovered classes and add tests.

## Development Workflow

### 1. Make Changes

Edit source code in `src/main/java/`

### 2. Run Tests (TDD)

```bash
mvn test
```

### 3. Verify Integration

```bash
mvn verify
```

### 4. Rebuild Container

```bash
mvn clean package -DskipTests
docker-compose up -d
```

### 5. Manual Testing

Use Swagger UI or curl commands above

### 6. Commit Changes

```bash
git add .
git commit -m "Description of changes"
git push origin 001-ocs-provisioning-service
```

## Production Deployment

### Build Production Image

```bash
mvn clean package -Pprod -DskipTests
```

This automatically builds the Docker image using the fabric8 docker-maven-plugin. The image will be tagged as:
`com.telecom.ocs.provisioning/ocs-provisioning-service:0.0.1-SNAPSHOT`

### Tag and Push to Registry

```bash
docker tag com.telecom.ocs.provisioning/ocs-provisioning-service:0.0.1-SNAPSHOT registry.example.com/ocs-provisioning-service:1.0.0
docker push registry.example.com/ocs-provisioning-service:1.0.0
```

### Deploy to Kubernetes

```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

Or use Helm chart (if available):

```bash
helm install ocs-provisioning ./helm-chart --values production-values.yaml
```

## Monitoring

### Application Metrics

```
http://localhost:8080/actuator/metrics
```

### Health Status

```
http://localhost:8080/actuator/health
```

### Application Info

```
http://localhost:8080/actuator/info
```

## Additional Resources

- OpenAPI Specification: `app-spec/ocs-provisioing-api.yml`
- Entity Documentation: `app-spec/docs/entities/`
- Data Model: `app-spec/data-model.md`
- Feature Specification: `specs/001-ocs-provisioning-service/spec.md`
- Implementation Plan: `specs/001-ocs-provisioning-service/plan.md`
- Constitution: `.specify/memory/constitution.md`

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review specification and data model documentation
3. Check application logs (`docker logs ocs-provisioning-service`)
4. Open issue in repository issue tracker
