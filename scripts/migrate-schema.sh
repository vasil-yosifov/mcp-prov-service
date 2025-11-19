#!/bin/bash
# migrate-schema.sh - Run Flyway migrations for OCS Provisioning Service
# This script applies pending database migrations

set -e  # Exit on error
set -u  # Exit on undefined variable

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values (can be overridden by environment variables)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ocs_provisioning}"
DB_USER="${DB_USER:-ocsuser}"
DB_PASSWORD="${DB_PASSWORD:-ocspass}"
FLYWAY_COMMAND="${1:-migrate}"  # Default to migrate, can be: info, validate, repair, clean

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

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# Show usage
usage() {
    cat <<EOF
Usage: $0 [COMMAND]

Flyway migration commands:
  migrate   - Apply pending migrations (default)
  info      - Show migration status and history
  validate  - Validate applied migrations against available ones
  repair    - Repair Flyway schema history table
  clean     - Drop all objects in schema (DANGEROUS - use with caution)

Environment Variables:
  DB_HOST          Database host (default: localhost)
  DB_PORT          Database port (default: 3306)
  DB_NAME          Database name (default: ocs_provisioning)
  DB_USER          Database user (default: ocsuser)
  DB_PASSWORD      Database password (default: ocspass)

Examples:
  $0                    # Apply pending migrations
  $0 info              # Show migration status
  $0 validate          # Validate migrations
  DB_HOST=prod-db $0   # Migrate on production database

EOF
}

# Check if Maven is available
check_maven() {
    if ! command -v mvn &> /dev/null; then
        log_error "Maven not found. Please install Maven."
        exit 1
    fi
}

# Check database connectivity
check_database() {
    log_info "Checking database connectivity to ${DB_HOST}:${DB_PORT}..."
    
    if ! command -v mysql &> /dev/null; then
        log_warn "mysql client not found - skipping connection test"
        log_warn "Proceeding with migration (will fail if database is unavailable)"
        return
    fi
    
    if ! mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" -e "SELECT 1" &> /dev/null; then
        log_error "Cannot connect to database at ${DB_HOST}:${DB_PORT}"
        log_error "Please check database credentials and connectivity."
        exit 1
    fi
    
    log_info "Database connection successful."
}

# Run Flyway command
run_flyway() {
    local command="$1"
    
    log_info "Running Flyway command: ${command}"
    log_debug "Database: ${DB_NAME} on ${DB_HOST}:${DB_PORT}"
    
    cd "${PROJECT_ROOT}"
    
    # Set Flyway configuration via environment variables
    export FLYWAY_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    export FLYWAY_USER="${DB_USER}"
    export FLYWAY_PASSWORD="${DB_PASSWORD}"
    export FLYWAY_LOCATIONS="filesystem:src/main/resources/db/migration"
    export FLYWAY_BASELINE_ON_MIGRATE=true
    export FLYWAY_VALIDATE_ON_MIGRATE=true
    
    # Run Maven Flyway plugin
    case "${command}" in
        migrate)
            mvn flyway:migrate -Dflyway.configFiles=
            ;;
        info)
            mvn flyway:info -Dflyway.configFiles=
            ;;
        validate)
            mvn flyway:validate -Dflyway.configFiles=
            ;;
        repair)
            log_warn "Running Flyway repair - this will fix schema history table"
            mvn flyway:repair -Dflyway.configFiles=
            ;;
        clean)
            log_error "DANGER: Flyway clean will DROP ALL OBJECTS in schema '${DB_NAME}'"
            read -p "Are you sure? Type 'yes' to continue: " confirm
            if [ "${confirm}" != "yes" ]; then
                log_info "Clean cancelled."
                exit 0
            fi
            mvn flyway:clean -Dflyway.configFiles=
            ;;
        *)
            log_error "Unknown Flyway command: ${command}"
            usage
            exit 1
            ;;
    esac
    
    log_info "Flyway ${command} completed successfully."
}

# Show migration status
show_status() {
    log_info "Current migration status:"
    
    if command -v mysql &> /dev/null; then
        mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" <<EOF
SELECT 
    installed_rank,
    version,
    description,
    type,
    script,
    installed_on,
    execution_time,
    success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
EOF
    else
        log_warn "mysql client not found - cannot show migration history"
    fi
}

# Main execution
main() {
    # Check for help flag
    if [ "${FLYWAY_COMMAND}" = "-h" ] || [ "${FLYWAY_COMMAND}" = "--help" ]; then
        usage
        exit 0
    fi
    
    log_info "=== OCS Provisioning Service - Schema Migration ==="
    log_info "Target: ${DB_NAME} on ${DB_HOST}:${DB_PORT}"
    log_info "Command: ${FLYWAY_COMMAND}"
    log_info ""
    
    check_maven
    check_database
    run_flyway "${FLYWAY_COMMAND}"
    
    if [ "${FLYWAY_COMMAND}" = "migrate" ]; then
        show_status
    fi
    
    log_info ""
    log_info "=== Migration Completed Successfully ==="
}

# Run main function
main "$@"
