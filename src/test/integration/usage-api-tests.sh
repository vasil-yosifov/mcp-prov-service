#!/bin/bash

# Usage API Integration Tests
# Tests all usage-related endpoints and use cases
# Usage: ./usage-api-tests.sh [--skip-cleanup]

# Parse command line arguments
SKIP_CLEANUP=false
for arg in "$@"; do
    case $arg in
        --skip-cleanup)
            SKIP_CLEANUP=true
            shift
            ;;
    esac
done

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080/ocs/prov/v1}"
CONTENT_TYPE="Content-Type: application/json"
TRANSACTION_ID="X-Transaction-ID"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Cleanup variables
CREATED_SUBSCRIBER_ID=""
CREATED_SUBSCRIPTION_ID=""
CREATED_BALANCE_IDS=()
CREATED_USAGE_IDS=()

# Helper functions
log_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((TESTS_PASSED++))
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((TESTS_FAILED++))
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

assert_equals() {
    local expected="$1"
    local actual="$2"
    local test_name="$3"
    
    ((TESTS_RUN++))
    if [ "$expected" = "$actual" ]; then
        log_success "$test_name"
    else
        log_error "$test_name - Expected: $expected, Got: $actual"
    fi
}

assert_contains() {
    local haystack="$1"
    local needle="$2"
    local test_name="$3"
    
    ((TESTS_RUN++))
    if [[ "$haystack" == *"$needle"* ]]; then
        log_success "$test_name"
    else
        log_error "$test_name - Expected to contain: $needle"
    fi
}

assert_not_empty() {
    local value="$1"
    local test_name="$2"
    
    ((TESTS_RUN++))
    if [ -n "$value" ]; then
        log_success "$test_name"
    else
        log_error "$test_name - Value is empty"
    fi
}

cleanup() {
    if [ "$SKIP_CLEANUP" = true ]; then
        log_info "Skipping cleanup (--skip-cleanup flag set)"
        log_info "Created subscriber: $CREATED_SUBSCRIBER_ID"
        log_info "Created subscription: $CREATED_SUBSCRIPTION_ID"
        log_info "Created balances: ${CREATED_BALANCE_IDS[@]}"
        log_info "Created usage records: ${CREATED_USAGE_IDS[@]}"
        return
    fi
    
    log_info "Cleaning up test data..."
    
    # Note: Usage records don't have a delete endpoint, they are immutable
    
    # Delete balances
    for balance_id in "${CREATED_BALANCE_IDS[@]}"; do
        curl -s -X DELETE "$BASE_URL/subscriptions/$CREATED_SUBSCRIPTION_ID/balances/$balance_id" \
            -H "$CONTENT_TYPE" \
            -H "$TRANSACTION_ID: test-cleanup-$(date +%s)" > /dev/null 2>&1 || true
    done
    
    # Delete subscription
    if [ -n "$CREATED_SUBSCRIPTION_ID" ]; then
        curl -s -X DELETE "$BASE_URL/subscriptions/$CREATED_SUBSCRIPTION_ID" \
            -H "$TRANSACTION_ID: test-cleanup-$(date +%s)" > /dev/null 2>&1 || true
    fi
    
    # Delete subscriber
    if [ -n "$CREATED_SUBSCRIBER_ID" ]; then
        curl -s -X DELETE "$BASE_URL/subscribers/$CREATED_SUBSCRIBER_ID" \
            -H "$TRANSACTION_ID: test-cleanup-$(date +%s)" > /dev/null 2>&1 || true
    fi
}

# Setup test data
setup_test_data() {
    log_info "Setting up test data..."
    
    # Generate unique MSISDN using timestamp
    local timestamp=$(date +%s)
    local unique_msisdn="9${timestamp:2}99"  # Will be 11-13 digits
    
    # Create subscriber
    local subscriber_payload=$(cat <<EOF
{
  "msisdn": "$unique_msisdn",
  "personalInfo": {
    "firstName": "Usage",
    "lastName": "Tester",
    "email": "usage.tester@example.com"
  },
  "services": {
    "voiceEnabled": true,
    "dataEnabled": true,
    "smsEnabled": true
  }
}
EOF
)
    
    log_info "Creating test subscriber..."
    local sub_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/subscribers" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-setup-sub-$timestamp" \
        -d "$subscriber_payload")
    
    local sub_body=$(echo "$sub_response" | sed '$d')
    local sub_status=$(echo "$sub_response" | tail -n 1)
    
    if [ "$sub_status" != "201" ]; then
        log_error "Failed to create subscriber: HTTP $sub_status"
        echo "$sub_body"
        exit 1
    fi
    
    CREATED_SUBSCRIBER_ID=$(echo "$sub_body" | grep -o '"subscriberId":"[^"]*"' | cut -d'"' -f4)
    log_info "Created subscriber: $CREATED_SUBSCRIBER_ID"
    
    # Create subscription
    local subscription_payload=$(cat <<EOF
{
  "subscriberId": "$CREATED_SUBSCRIBER_ID",
  "offerId": "OFFER_VOICE_DATA_SMS"
}
EOF
)
    
    log_info "Creating test subscription..."
    local subs_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/subscriptions" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-setup-subs-$timestamp" \
        -d "$subscription_payload")
    
    local subs_body=$(echo "$subs_response" | sed '$d')
    local subs_status=$(echo "$subs_response" | tail -n 1)
    
    if [ "$subs_status" != "201" ]; then
        log_error "Failed to create subscription: HTTP $subs_status"
        echo "$subs_body"
        cleanup
        exit 1
    fi
    
    CREATED_SUBSCRIPTION_ID=$(echo "$subs_body" | grep -o '"subscriptionId":"[^"]*"' | cut -d'"' -f4)
    log_info "Created subscription: $CREATED_SUBSCRIPTION_ID"
    
    # Create VOICE balance (ALLOWANCE type - will be deducted)
    local voice_balance_payload=$(cat <<EOF
{
  "balanceType": "ALLOWANCE",
  "unitType": "SECONDS",
  "balanceAmount": 3600,
  "balanceAvailable": 3600,
  "expirationDate": "2025-12-31T23:59:59Z",
  "isRolloverAllowed": false,
  "isRecurring": false,
  "isGroupBalance": false
}
EOF
)
    
    log_info "Creating VOICE balance..."
    local voice_bal_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/subscriptions/$CREATED_SUBSCRIPTION_ID/balances" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-setup-voice-bal-$timestamp" \
        -d "$voice_balance_payload")
    
    local voice_bal_body=$(echo "$voice_bal_response" | sed '$d')
    local voice_bal_status=$(echo "$voice_bal_response" | tail -n 1)
    
    if [ "$voice_bal_status" != "201" ]; then
        log_error "Failed to create VOICE balance: HTTP $voice_bal_status"
        echo "$voice_bal_body"
        cleanup
        exit 1
    fi
    
    local voice_balance_id=$(echo "$voice_bal_body" | grep -o '"balanceId":"[^"]*"' | cut -d'"' -f4)
    CREATED_BALANCE_IDS+=("$voice_balance_id")
    log_info "Created VOICE balance: $voice_balance_id"
    
    # Create DATA balance (ALLOWANCE type - will be deducted)
    local data_balance_payload=$(cat <<EOF
{
  "balanceType": "ALLOWANCE",
  "unitType": "BYTES",
  "balanceAmount": 10737418240,
  "balanceAvailable": 10737418240,
  "expirationDate": "2025-12-31T23:59:59Z",
  "isRolloverAllowed": false,
  "isRecurring": false,
  "isGroupBalance": false
}
EOF
)
    
    log_info "Creating DATA balance..."
    local data_bal_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/subscriptions/$CREATED_SUBSCRIPTION_ID/balances" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-setup-data-bal-$timestamp" \
        -d "$data_balance_payload")
    
    local data_bal_body=$(echo "$data_bal_response" | sed '$d')
    local data_bal_status=$(echo "$data_bal_response" | tail -n 1)
    
    if [ "$data_bal_status" != "201" ]; then
        log_error "Failed to create DATA balance: HTTP $data_bal_status"
        echo "$data_bal_body"
        cleanup
        exit 1
    fi
    
    local data_balance_id=$(echo "$data_bal_body" | grep -o '"balanceId":"[^"]*"' | cut -d'"' -f4)
    CREATED_BALANCE_IDS+=("$data_balance_id")
    log_info "Created DATA balance: $data_balance_id"
    
    # Create SMS balance (COUNTER type - will be incremented)
    local sms_balance_payload=$(cat <<EOF
{
  "balanceType": "COUNTER",
  "unitType": "EVENTS",
  "balanceAmount": 0,
  "balanceAvailable": 0,
  "expirationDate": "2025-12-31T23:59:59Z",
  "isRolloverAllowed": false,
  "isRecurring": false,
  "isGroupBalance": false
}
EOF
)
    
    log_info "Creating SMS balance..."
    local sms_bal_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/subscriptions/$CREATED_SUBSCRIPTION_ID/balances" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-setup-sms-bal-$timestamp" \
        -d "$sms_balance_payload")
    
    local sms_bal_body=$(echo "$sms_bal_response" | sed '$d')
    local sms_bal_status=$(echo "$sms_bal_response" | tail -n 1)
    
    if [ "$sms_bal_status" != "201" ]; then
        log_error "Failed to create SMS balance: HTTP $sms_bal_status"
        echo "$sms_bal_body"
        cleanup
        exit 1
    fi
    
    local sms_balance_id=$(echo "$sms_bal_body" | grep -o '"balanceId":"[^"]*"' | cut -d'"' -f4)
    CREATED_BALANCE_IDS+=("$sms_balance_id")
    log_info "Created SMS balance: $sms_balance_id"
    
    # Create MMS balance (COUNTER type - will be incremented)
    local mms_balance_payload=$(cat <<EOF
{
  "balanceType": "COUNTER",
  "unitType": "EVENTS",
  "balanceAmount": 0,
  "balanceAvailable": 0,
  "expirationDate": "2025-12-31T23:59:59Z",
  "isRolloverAllowed": false,
  "isRecurring": false,
  "isGroupBalance": false
}
EOF
)
    
    log_info "Creating MMS balance..."
    local mms_bal_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/subscriptions/$CREATED_SUBSCRIPTION_ID/balances" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-setup-mms-bal-$timestamp" \
        -d "$mms_balance_payload")
    
    local mms_bal_body=$(echo "$mms_bal_response" | sed '$d')
    local mms_bal_status=$(echo "$mms_bal_response" | tail -n 1)
    
    if [ "$mms_bal_status" != "201" ]; then
        log_error "Failed to create MMS balance: HTTP $mms_bal_status"
        echo "$mms_bal_body"
        cleanup
        exit 1
    fi
    
    local mms_balance_id=$(echo "$mms_bal_body" | grep -o '"balanceId":"[^"]*"' | cut -d'"' -f4)
    CREATED_BALANCE_IDS+=("$mms_balance_id")
    log_info "Created MMS balance: $mms_balance_id"
    
    log_info "Test data setup complete"
}

# Test functions

test_create_voice_usage() {
    log_info "Testing: Create VOICE usage record"
    
    local usage_id="usage-voice-$(uuidgen)"
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    local voice_balance_id="${CREATED_BALANCE_IDS[0]}"
    
    local usage_payload=$(cat <<EOF
{
  "usageId": "$usage_id",
  "chargedPartyId": "$CREATED_SUBSCRIBER_ID",
  "chargedMsisdn": "436602238811",
  "aParty": "436602238811",
  "bParty": "436602238822",
  "usageType": "VOICE",
  "recordType": "STOP",
  "recordOpeningTime": "2024-06-15T10:00:00Z",
  "recordClosingTime": "2024-06-15T10:05:00Z",
  "durationSeconds": 300,
  "volumeUsage": 300,
  "impactedBalanceId": "$voice_balance_id",
  "offerId": "OFFER123456"
}
EOF
)
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/usage" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-voice-usage-$(date +%s)" \
        -d "$usage_payload")
    
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)
    
    assert_equals "201" "$status" "VOICE usage - HTTP status 201"
    assert_contains "$body" "$usage_id" "VOICE usage - usageId in response"
    assert_contains "$body" "VOICE" "VOICE usage - usageType in response"
    assert_contains "$body" "balanceValueBefore" "VOICE usage - balanceValueBefore in response"
    assert_contains "$body" "balanceValueAfter" "VOICE usage - balanceValueAfter in response"
    
    CREATED_USAGE_IDS+=("$usage_id")
}

test_create_data_usage() {
    log_info "Testing: Create DATA usage record"
    
    local usage_id="usage-data-$(uuidgen)"
    local data_balance_id="${CREATED_BALANCE_IDS[1]}"
    
    local usage_payload=$(cat <<EOF
{
  "usageId": "$usage_id",
  "chargedPartyId": "$CREATED_SUBSCRIBER_ID",
  "chargedMsisdn": "436602238811",
  "aParty": "436602238811",
  "bParty": "internet.telco.com",
  "usageType": "DATA",
  "recordType": "STOP",
  "recordOpeningTime": "2024-06-15T11:00:00Z",
  "recordClosingTime": "2024-06-15T11:30:00Z",
  "durationSeconds": 1800,
  "volumeUsage": 104857600,
  "impactedBalanceId": "$data_balance_id",
  "offerId": "OFFER789012"
}
EOF
)
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/usage" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-data-usage-$(date +%s)" \
        -d "$usage_payload")
    
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)
    
    assert_equals "201" "$status" "DATA usage - HTTP status 201"
    assert_contains "$body" "$usage_id" "DATA usage - usageId in response"
    assert_contains "$body" "DATA" "DATA usage - usageType in response"
    assert_contains "$body" "balanceValueBefore" "DATA usage - balanceValueBefore in response"
    assert_contains "$body" "balanceValueAfter" "DATA usage - balanceValueAfter in response"
    
    CREATED_USAGE_IDS+=("$usage_id")
}

test_create_sms_usage() {
    log_info "Testing: Create SMS usage record"
    
    local usage_id="usage-sms-$(uuidgen)"
    local sms_balance_id="${CREATED_BALANCE_IDS[2]}"
    
    local usage_payload=$(cat <<EOF
{
  "usageId": "$usage_id",
  "chargedPartyId": "$CREATED_SUBSCRIBER_ID",
  "chargedMsisdn": "436602238811",
  "aParty": "436602238811",
  "bParty": "436602238833",
  "usageType": "SMS",
  "recordType": "EVENT",
  "recordOpeningTime": "2024-06-15T12:00:00Z",
  "recordClosingTime": "2024-06-15T12:00:00Z",
  "durationSeconds": 0,
  "volumeUsage": 1,
  "impactedBalanceId": "$sms_balance_id",
  "offerId": "OFFER345678"
}
EOF
)
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/usage" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-sms-usage-$(date +%s)" \
        -d "$usage_payload")
    
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)
    
    assert_equals "201" "$status" "SMS usage - HTTP status 201"
    assert_contains "$body" "$usage_id" "SMS usage - usageId in response"
    assert_contains "$body" "SMS" "SMS usage - usageType in response"
    assert_contains "$body" "balanceValueBefore" "SMS usage - balanceValueBefore in response"
    assert_contains "$body" "balanceValueAfter" "SMS usage - balanceValueAfter in response"
    
    CREATED_USAGE_IDS+=("$usage_id")
}

test_create_mms_usage() {
    log_info "Testing: Create MMS usage record"
    
    local usage_id="usage-mms-$(uuidgen)"
    local mms_balance_id="${CREATED_BALANCE_IDS[3]}"
    
    local usage_payload=$(cat <<EOF
{
  "usageId": "$usage_id",
  "chargedPartyId": "$CREATED_SUBSCRIBER_ID",
  "chargedMsisdn": "436602238811",
  "aParty": "436602238811",
  "bParty": "436602238844",
  "usageType": "MMS",
  "recordType": "EVENT",
  "recordOpeningTime": "2024-06-15T13:00:00Z",
  "recordClosingTime": "2024-06-15T13:00:00Z",
  "durationSeconds": 0,
  "volumeUsage": 1,
  "impactedBalanceId": "$mms_balance_id",
  "offerId": "OFFER456789"
}
EOF
)
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/usage" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-mms-usage-$(date +%s)" \
        -d "$usage_payload")
    
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)
    
    assert_equals "201" "$status" "MMS usage - HTTP status 201"
    assert_contains "$body" "$usage_id" "MMS usage - usageId in response"
    assert_contains "$body" "MMS" "MMS usage - usageType in response"
    assert_contains "$body" "balanceValueBefore" "MMS usage - balanceValueBefore in response"
    assert_contains "$body" "balanceValueAfter" "MMS usage - balanceValueAfter in response"
    
    CREATED_USAGE_IDS+=("$usage_id")
}

test_duplicate_usage_id() {
    log_info "Testing: Duplicate usage ID returns 409"
    
    # Use the first created usage ID (should already exist)
    local usage_id="${CREATED_USAGE_IDS[0]}"
    local voice_balance_id="${CREATED_BALANCE_IDS[0]}"
    
    local usage_payload=$(cat <<EOF
{
  "usageId": "$usage_id",
  "chargedPartyId": "$CREATED_SUBSCRIBER_ID",
  "chargedMsisdn": "436602238811",
  "aParty": "436602238811",
  "bParty": "436602238822",
  "usageType": "VOICE",
  "recordType": "STOP",
  "recordOpeningTime": "2024-06-15T14:00:00Z",
  "recordClosingTime": "2024-06-15T14:05:00Z",
  "durationSeconds": 300,
  "volumeUsage": 300,
  "impactedBalanceId": "$voice_balance_id",
  "offerId": "OFFER123456"
}
EOF
)
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/usage" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-duplicate-usage-$(date +%s)" \
        -d "$usage_payload")
    
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)
    
    assert_equals "409" "$status" "Duplicate usage - HTTP status 409"
    assert_contains "$body" "already exists" "Duplicate usage - error message"
}

test_invalid_subscriber_id() {
    log_info "Testing: Invalid subscriber ID returns 404"
    
    local usage_id="usage-invalid-sub-$(uuidgen)"
    local voice_balance_id="${CREATED_BALANCE_IDS[0]}"
    
    local usage_payload=$(cat <<EOF
{
  "usageId": "$usage_id",
  "chargedPartyId": "NONEXISTENT_SUB",
  "chargedMsisdn": "436602238811",
  "aParty": "436602238811",
  "bParty": "436602238822",
  "usageType": "VOICE",
  "recordType": "STOP",
  "recordOpeningTime": "2024-06-15T15:00:00Z",
  "recordClosingTime": "2024-06-15T15:05:00Z",
  "durationSeconds": 300,
  "volumeUsage": 300,
  "impactedBalanceId": "$voice_balance_id",
  "offerId": "OFFER123456"
}
EOF
)
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/usage" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-invalid-sub-$(date +%s)" \
        -d "$usage_payload")
    
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)
    
    assert_equals "404" "$status" "Invalid subscriber - HTTP status 404"
    assert_contains "$body" "Subscriber not found" "Invalid subscriber - error message"
}

test_invalid_balance_id() {
    log_info "Testing: Invalid balance ID returns 404"
    
    local usage_id="usage-invalid-bal-$(uuidgen)"
    
    local usage_payload=$(cat <<EOF
{
  "usageId": "$usage_id",
  "chargedPartyId": "$CREATED_SUBSCRIBER_ID",
  "chargedMsisdn": "436602238811",
  "aParty": "436602238811",
  "bParty": "436602238822",
  "usageType": "VOICE",
  "recordType": "STOP",
  "recordOpeningTime": "2024-06-15T16:00:00Z",
  "recordClosingTime": "2024-06-15T16:05:00Z",
  "durationSeconds": 300,
  "volumeUsage": 300,
  "impactedBalanceId": "NONEXISTENT_BALANCE",
  "offerId": "OFFER123456"
}
EOF
)
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/usage" \
        -H "$CONTENT_TYPE" \
        -H "$TRANSACTION_ID: test-invalid-bal-$(date +%s)" \
        -d "$usage_payload")
    
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)
    
    assert_equals "404" "$status" "Invalid balance - HTTP status 404"
    assert_contains "$body" "Balance not found" "Invalid balance - error message"
}

test_get_subscriber_usage() {
    log_info "Testing: GET subscriber usage"
    
    local response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subscribers/$CREATED_SUBSCRIBER_ID/usage" \
        -H "$TRANSACTION_ID: test-get-usage-$(date +%s)")
    
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)
    
    assert_equals "200" "$status" "GET usage - HTTP status 200"
    assert_contains "$body" "usageId" "GET usage - contains usageId"
    assert_contains "$body" "VOICE" "GET usage - contains VOICE usage"
    assert_contains "$body" "DATA" "GET usage - contains DATA usage"
    assert_contains "$body" "SMS" "GET usage - contains SMS usage"
    assert_contains "$body" "MMS" "GET usage - contains MMS usage"
}

test_get_usage_with_pagination() {
    log_info "Testing: GET usage with pagination (limit=2)"
    
    local response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subscribers/$CREATED_SUBSCRIBER_ID/usage?limit=2&offset=0" \
        -H "$TRANSACTION_ID: test-get-usage-page-$(date +%s)")
    
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)
    
    assert_equals "200" "$status" "GET usage pagination - HTTP status 200"
    
    # Count usage records in response (rough check)
    local usage_count=$(echo "$body" | grep -o '"usageId"' | wc -l | tr -d ' ')
    
    ((TESTS_RUN++))
    if [ "$usage_count" -le 2 ]; then
        log_success "GET usage pagination - respects limit parameter (found $usage_count records)"
    else
        log_error "GET usage pagination - should have max 2 records, found $usage_count"
    fi
}

test_get_nonexistent_subscriber_usage() {
    log_info "Testing: GET usage for nonexistent subscriber returns 404"
    
    local response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subscribers/NONEXISTENT_SUB/usage" \
        -H "$TRANSACTION_ID: test-get-nonexistent-$(date +%s)")
    
    local body=$(echo "$response" | sed '$d')
    local status=$(echo "$response" | tail -n 1)
    
    assert_equals "404" "$status" "GET nonexistent subscriber usage - HTTP status 404"
    assert_contains "$body" "Subscriber not found" "GET nonexistent subscriber usage - error message"
}

test_balance_updates() {
    log_info "Testing: Balance updates after usage"
    
    # Get all balances for the subscription
    local voice_balance_id="${CREATED_BALANCE_IDS[0]}"
    local all_balances=$(curl -s -X GET "$BASE_URL/subscriptions/$CREATED_SUBSCRIPTION_ID/balances" \
        -H "$TRANSACTION_ID: test-get-balances-$(date +%s)")
    
    # Extract VOICE balance (ALLOWANCE - should be deducted)
    # Find the balance object by ID, then extract balanceAvailable from that object
    local voice_balance_obj=$(echo "$all_balances" | jq -r ".[] | select(.balanceId == \"$voice_balance_id\")")
    local voice_available=$(echo "$voice_balance_obj" | jq -r '.balanceAvailable')
    
    # Should be less than initial 3600 (we used 300 seconds)
    ((TESTS_RUN++))
    if [ -n "$voice_available" ] && [ "$voice_available" != "null" ] && [ "$(echo "$voice_available < 3600" | bc)" -eq 1 ]; then
        log_success "VOICE balance - ALLOWANCE was deducted (now: $voice_available, was: 3600)"
    else
        log_error "VOICE balance - ALLOWANCE was not deducted (value: $voice_available)"
    fi
    
    # Extract SMS balance (COUNTER - should be incremented)
    local sms_balance_id="${CREATED_BALANCE_IDS[2]}"
    local sms_balance_obj=$(echo "$all_balances" | jq -r ".[] | select(.balanceId == \"$sms_balance_id\")")
    local sms_available=$(echo "$sms_balance_obj" | jq -r '.balanceAvailable')
    
    # Should be greater than initial 0 (we sent 1 SMS)
    ((TESTS_RUN++))
    if [ -n "$sms_available" ] && [ "$sms_available" != "null" ] && [ "$(echo "$sms_available > 0" | bc)" -eq 1 ]; then
        log_success "SMS balance - COUNTER was incremented (now: $sms_available, was: 0)"
    else
        log_error "SMS balance - COUNTER was not incremented (value: $sms_available)"
    fi
}

# Trap to ensure cleanup runs
trap cleanup EXIT

# Main execution
echo "========================================"
echo "    Usage API Integration Tests"
echo "========================================"
echo ""

# Setup
setup_test_data
echo ""

# Run tests
test_create_voice_usage
echo ""
test_create_data_usage
echo ""
test_create_sms_usage
echo ""
test_create_mms_usage
echo ""
test_duplicate_usage_id
echo ""
test_invalid_subscriber_id
echo ""
test_invalid_balance_id
echo ""
test_get_subscriber_usage
echo ""
test_get_usage_with_pagination
echo ""
test_get_nonexistent_subscriber_usage
echo ""
test_balance_updates
echo ""

# Summary
echo "========================================"
echo "           Test Summary"
echo "========================================"
echo "Total Tests: $TESTS_RUN"
echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Failed: $TESTS_FAILED${NC}"
echo "========================================"

# Exit with failure if any tests failed
if [ $TESTS_FAILED -gt 0 ]; then
    exit 1
fi

exit 0
