#!/bin/bash

echo "K6 Load Testing with Grafana Visualization"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Start Docker services
echo -e "${YELLOW}Starting Docker services...${NC}"
docker compose up -d influxdb grafana

# Wait for InfluxDB to be healthy
echo -e "${YELLOW}Waiting for InfluxDB to be ready...${NC}"
until docker compose exec -T influxdb influx -execute "SHOW DATABASES" 2>/dev/null | grep -q k6; do
    echo -n "."
    sleep 2
done
echo -e "${GREEN}InfluxDB is ready!${NC}"

# Wait for Grafana to be ready
echo -e "${YELLOW}Waiting for Grafana to be ready...${NC}"
until curl -s http://localhost:3000/api/health | grep -q "ok"; do
    echo -n "."
    sleep 2
done
echo -e "${GREEN}Grafana is ready!${NC}"

echo ""
echo -e "${GREEN}Grafana is available at: http://localhost:3000${NC}"
echo -e "${GREEN}Login: admin / admin123${NC}"
echo ""

# Function to run K6 test with InfluxDB output
run_k6_test() {
    local test_file=$1
    local test_name=$(basename "$test_file" .js)
    
    echo -e "${YELLOW}Running test: $test_name${NC}"
    
    # Run K6 with InfluxDB output
    k6 run \
        --out influxdb=http://localhost:8086/k6 \
        --tag testname="$test_name" \
        "$test_file"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Test $test_name completed successfully${NC}"
    else
        echo -e "${RED}Test $test_name failed${NC}"
    fi
}

# Check if a specific test file was provided
if [ $# -eq 1 ]; then
    if [ -f "$1" ]; then
        run_k6_test "$1"
    else
        echo -e "${RED}Error: Test file '$1' not found${NC}"
        exit 1
    fi
else
    # Show menu
    echo "Select a test to run:"
    echo "1) Order Load Test"
    echo "2) Order Stress Test"
    echo "3) Order Peak Test"
    echo "4) Balance Charge Load Test"
    echo "5) Balance Charge Stress Test"
    echo "6) E2E Shopping Scenario"
    echo "7) Concurrent Operations Test"
    echo "8) Custom test file"
    echo ""
    read -p "Enter your choice (1-8): " choice

    case $choice in
        1)
            run_k6_test "k6-tests/scenarios/order/order-load-test-influx.js"
            ;;
        2)
            run_k6_test "k6-tests/scenarios/order/order-stress-test.js"
            ;;
        3)
            run_k6_test "k6-tests/scenarios/order/order-peak-test.js"
            ;;
        4)
            run_k6_test "k6-tests/scenarios/balance/charge-load-test.js"
            ;;
        5)
            run_k6_test "k6-tests/scenarios/balance/charge-stress-test.js"
            ;;
        6)
            run_k6_test "k6-tests/scenarios/integrated/e2e-shopping-scenario.js"
            ;;
        7)
            run_k6_test "k6-tests/scenarios/integrated/concurrent-operations-test.js"
            ;;
        8)
            read -p "Enter the path to your test file: " custom_file
            if [ -f "$custom_file" ]; then
                run_k6_test "$custom_file"
            else
                echo -e "${RED}Error: File '$custom_file' not found${NC}"
                exit 1
            fi
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            exit 1
            ;;
    esac
fi

echo ""
echo -e "${GREEN}Test results are being visualized in Grafana${NC}"
echo -e "${GREEN}Open http://localhost:3000 to view the dashboard${NC}"
echo ""
echo "Press Ctrl+C to stop monitoring, or run 'docker compose down' to stop all services"