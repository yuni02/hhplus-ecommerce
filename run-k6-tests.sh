#!/bin/bash

echo "🚀 K6 부하 테스트 실행 스크립트"
echo "================================"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 테스트 선택 메뉴
echo -e "\n${GREEN}실행할 테스트를 선택하세요:${NC}"
echo "1) 포인트 충전 - Load Test (일반 부하)"
echo "2) 포인트 충전 - Stress Test (한계 테스트)"
echo "3) 포인트 충전 - Peak Test (급증 테스트)"
echo "4) 주문 처리 - Load Test"
echo "5) 주문 처리 - Stress Test"
echo "6) 주문 처리 - Peak Test (플래시 세일)"
echo "7) E2E 쇼핑 시나리오"
echo "8) 동시성 테스트 (데드락/레이스 컨디션)"
echo "9) 내구성 테스트 (30분 장시간)"
echo "0) 종료"

read -p "선택 (0-9): " choice

# BASE_URL 설정
BASE_URL=${BASE_URL:-http://localhost:8083}
echo -e "\n${YELLOW}Target URL: $BASE_URL${NC}"

case $choice in
    1)
        echo -e "\n${GREEN}📊 포인트 충전 Load Test 실행${NC}"
        k6 run -e BASE_URL=$BASE_URL k6-tests/scenarios/balance/charge-load-test.js
        ;;
    2)
        echo -e "\n${GREEN}💥 포인트 충전 Stress Test 실행${NC}"
        k6 run -e BASE_URL=$BASE_URL k6-tests/scenarios/balance/charge-stress-test.js
        ;;
    3)
        echo -e "\n${GREEN}⚡ 포인트 충전 Peak Test 실행${NC}"
        k6 run -e BASE_URL=$BASE_URL k6-tests/scenarios/balance/charge-peak-test.js
        ;;
    4)
        echo -e "\n${GREEN}🛒 주문 처리 Load Test 실행${NC}"
        k6 run -e BASE_URL=$BASE_URL k6-tests/scenarios/order/order-load-test.js
        ;;
    5)
        echo -e "\n${GREEN}🔥 주문 처리 Stress Test 실행${NC}"
        k6 run -e BASE_URL=$BASE_URL k6-tests/scenarios/order/order-stress-test.js
        ;;
    6)
        echo -e "\n${GREEN}🎯 주문 처리 Peak Test (플래시 세일) 실행${NC}"
        k6 run -e BASE_URL=$BASE_URL k6-tests/scenarios/order/order-peak-test.js
        ;;
    7)
        echo -e "\n${GREEN}🔄 E2E 쇼핑 시나리오 실행${NC}"
        k6 run -e BASE_URL=$BASE_URL k6-tests/scenarios/integrated/e2e-shopping-scenario.js
        ;;
    8)
        echo -e "\n${GREEN}⚠️ 동시성 테스트 실행${NC}"
        echo -e "${YELLOW}주의: 데드락 및 레이스 컨디션을 테스트합니다${NC}"
        k6 run -e BASE_URL=$BASE_URL k6-tests/scenarios/integrated/concurrent-operations-test.js
        ;;
    9)
        echo -e "\n${GREEN}⏱️ 내구성 테스트 실행 (30분)${NC}"
        echo -e "${YELLOW}주의: 이 테스트는 30분간 실행됩니다${NC}"
        read -p "계속하시겠습니까? (y/n): " confirm
        if [ "$confirm" = "y" ]; then
            k6 run -e BASE_URL=$BASE_URL k6-tests/scenarios/integrated/endurance-test.js
        fi
        ;;
    0)
        echo -e "${GREEN}테스트를 종료합니다${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}잘못된 선택입니다${NC}"
        exit 1
        ;;
esac

echo -e "\n${GREEN}✅ 테스트가 완료되었습니다${NC}"