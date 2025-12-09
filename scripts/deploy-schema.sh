#!/bin/bash
# deploy-schema.sh - Deploy initial database schema for OCS Provisioning Service
# This script creates the database and runs Flyway migrations

set -e  # Exit on error
set -u  # Exit on undefined variable

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values (can be overridden by environment variables)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ocs_provisioning}"
DB_USER="${DB_USER:-ocsuser}"
DB_PASSWORD="${DB_PASSWORD:-ocspass}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-rootpass}"

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if MySQL is accessible
check_mysql_connection() {
    log_info "Checking MySQL connection to ${DB_HOST}:${DB_PORT}..."
    
    if ! command -v mysql &> /dev/null; then
        log_error "mysql client not found. Please install mysql-client."
        exit 1
    fi
    
    if ! mysql -h"${DB_HOST}" -P"${DB_PORT}" -uroot -p"${DB_ROOT_PASSWORD}" -e "SELECT 1" &> /dev/null; then
        log_error "Cannot connect to MySQL at ${DB_HOST}:${DB_PORT}"
        log_error "Please ensure MySQL is running and root password is correct."
        exit 1
    fi
    
    log_info "MySQL connection successful."
}

# Create database if it doesn't exist
create_database() {
    log_info "Creating database '${DB_NAME}' if it doesn't exist..."
    
    mysql -h"${DB_HOST}" -P"${DB_PORT}" -uroot -p"${DB_ROOT_PASSWORD}" <<EOF
CREATE DATABASE IF NOT EXISTS ${DB_NAME}
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
EOF
    
    log_info "Database '${DB_NAME}' ready."
}

# Create database user and grant privileges
create_user() {
    log_info "Creating user '${DB_USER}' and granting privileges..."
    
    mysql -h"${DB_HOST}" -P"${DB_PORT}" -uroot -p"${DB_ROOT_PASSWORD}" <<EOF
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
EOF
    
    log_info "User '${DB_USER}' configured with full access to '${DB_NAME}'."
}

# Run Flyway migrations
run_migrations() {
    log_info "Running Flyway migrations..."
    
    cd "${PROJECT_ROOT}"
    
    # Set Flyway configuration via environment variables
    export FLYWAY_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    export FLYWAY_USER="${DB_USER}"
    export FLYWAY_PASSWORD="${DB_PASSWORD}"
    export FLYWAY_LOCATIONS="filesystem:src/main/resources/db/migration"
    export FLYWAY_BASELINE_ON_MIGRATE=true
    
    # Run Maven Flyway plugin
    if ! mvn flyway:migrate -Dflyway.configFiles= ; then
        log_error "Flyway migration failed!"
        exit 1
    fi
    
    log_info "Flyway migrations completed successfully."
}

# Verify schema deployment
verify_schema() {
    log_info "Verifying schema deployment..."
    
    TABLES=$(mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -sN -e "SHOW TABLES;")
    TABLE_COUNT=$(echo "${TABLES}" | wc -l | tr -d ' ')
    
    log_info "Found ${TABLE_COUNT} tables in database '${DB_NAME}':"
    echo "${TABLES}" | sed 's/^/  - /'
    
    # Expected minimum table count (7 core tables + flyway_schema_history + junction tables)
    EXPECTED_MIN_TABLES=10
    
    if [ "${TABLE_COUNT}" -lt "${EXPECTED_MIN_TABLES}" ]; then
        log_warn "Expected at least ${EXPECTED_MIN_TABLES} tables, found ${TABLE_COUNT}."
        log_warn "Schema deployment may be incomplete."
    else
        log_info "Schema deployment verification passed."
    fi
}

# Main execution
main() {
    log_info "=== OCS Provisioning Service - Schema Deployment ==="
    log_info "Database: ${DB_NAME} on ${DB_HOST}:${DB_PORT}"
    log_info ""
    
    check_mysql_connection
    create_database
    create_user
    run_migrations
    verify_schema
    
    log_info ""
    log_info "=== Schema Deployment Completed Successfully ==="
    log_info "Connection String: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
    log_info "Database User: ${DB_USER}"
}

# Run main function
main "$@"
