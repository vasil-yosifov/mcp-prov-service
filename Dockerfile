FROM eclipse-temurin:17-jre

LABEL maintainer="OCS Provisioning Team"
LABEL description="OCS Provisioning Service - Telecom Subscriber Profile Management"
LABEL version="0.0.1-SNAPSHOT"

# JAR file name is supplied by the Maven plugin as a build-arg
ARG JAR_FILE=target/ocs-provisioning-service-0.0.1-SNAPSHOT.jar

# Install wget for healthcheck
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -g 1001 ocsapp && \
    useradd -u 1001 -g 1001 -s /bin/sh -m ocsapp

WORKDIR /app

# Copy the JAR file built by Maven
COPY ${JAR_FILE} ocs-provisioning-service.jar

# Change ownership to non-root user
RUN chown -R ocsapp:ocsapp /app

# Switch to non-root user
USER ocsapp

EXPOSE 8080

# Health check using the /health-check endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health-check || exit 1

# JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/ocs-provisioning-service.jar"]
