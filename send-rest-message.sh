#!/bin/bash

# Integration Tests for Subscriber API (T047-T054)
# Tests the implementation of User Story 1: Subscriber Provisioning and Lifecycle Management

set -e

BASE_URL="http://localhost:8080/ocs/prov/v1"
PASSED=0
FAILED=0

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "REST Message Sender"
echo "Send HTTP requests to the SPR API"
echo "=========================================="
echo ""

# Helper function to send REST messages
send_rest_message() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    echo "Sending $method request to $endpoint..."
    
    if [ "$method" == "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL$endpoint")
    elif [ "$method" == "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    elif [ "$method" == "PATCH" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    elif [ "$method" == "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL$endpoint")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    # Check if status code is 2xx (success)
    if [[ "$http_code" =~ ^2[0-9]{2}$ ]]; then
        echo -e "${GREEN}✓ SUCCESS${NC} (Status: $http_code)"
        if [ ! -z "$body" ] && [ "$body" != "null" ]; then
            echo -e "${GREEN}Response: $body${NC}"
        fi
    else
        echo -e "${RED}✗ ERROR${NC} (Status: $http_code)"
        echo -e "${RED}Response: $body${NC}"
    fi
    echo ""
}

# Validate command line arguments
if [ $# -ne 3 ]; then
    echo -e "${RED}Error: Invalid number of arguments${NC}"
    echo "Usage: $0 <METHOD> <ENDPOINT> <DATA>"
    echo "  METHOD: HTTP method (GET, POST, PATCH, DELETE)"
    echo "  ENDPOINT: API endpoint path (e.g., /subscribers/)"
    echo "  DATA: JSON payload (use '{}' or '' for no data)"
    exit 1
fi

METHOD=$1
ENDPOINT=$2
DATA=$3

# Validate HTTP method
if [[ ! "$METHOD" =~ ^(GET|POST|PATCH|DELETE)$ ]]; then
    echo -e "${RED}Error: Invalid HTTP method${NC}"
    echo "Allowed methods: GET, POST, PATCH, DELETE"
    exit 1
fi

# Validate endpoint format
if [[ ! "$ENDPOINT" =~ ^/ ]]; then
    echo -e "${RED}Error: Endpoint must start with '/'${NC}"
    exit 1
fi

# Call send_rest_message with provided parameters
send_rest_message "$METHOD" "$ENDPOINT" "$DATA"
