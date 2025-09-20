
#!/bin/bash

echo "🚀 이커머스 시스템 부하 테스트 자동 실행 스크립트"
echo "================================================"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Step 1: Docker 서비스 시작
echo -e "\n${BLUE}📦 Step 1: Docker 서비스 시작${NC}"
echo "Docker Compose로 MySQL, Redis, Kafka 시작 중..."
docker-compose down
docker-compose up -d

# Step 2: DB 초기화 대기
echo -e "\n${BLUE}⏳ Step 2: 데이터베이스 초기화 대기 (30초)${NC}"
echo "MySQL이 완전히 시작되고 샘플 데이터가 로드될 때까지 대기..."
sleep 30

# Step 3: 데이터 확인
echo -e "\n${BLUE}📊 Step 3: 샘플 데이터 확인${NC}"
docker exec -it $(docker-compose ps -q mysql) mysql -uapplication -papplication -e "
USE ecommerce;
SELECT 'Users count:', COUNT(*) FROM users;
SELECT 'Products count:', COUNT(*) FROM products;
SELECT 'Balances count:', COUNT(*) FROM balances;
SELECT 'Coupons count:', COUNT(*) FROM coupons;
SELECT 'Product stock status:' as 'Stock Info';
SELECT product_id, name, stock, price FROM products WHERE stock > 0 LIMIT 10;
"

# Step 4: Spring Boot 애플리케이션 백그라운드 실행
echo -e "\n${BLUE}🌱 Step 4: Spring Boot 애플리케이션 시작${NC}"
echo "애플리케이션 시작 중... (백그라운드 실행)"
./gradlew bootRun > spring-boot.log 2>&1 &
SPRING_PID=$!
echo "Spring Boot PID: $SPRING_PID"

# Step 5: 애플리케이션 준비 대기
echo -e "\n${BLUE}⏳ Step 5: 애플리케이션 준비 대기${NC}"
echo "애플리케이션이 완전히 시작될 때까지 대기 중..."

for i in {1..60}; do
    if curl -f http://localhost:8083/actuator/health >/dev/null 2>&1; then
        echo -e "\n${GREEN}✅ 애플리케이션이 준비되었습니다!${NC}"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "\n${RED}❌ 애플리케이션 시작 타임아웃${NC}"
        echo "로그 확인: tail -f spring-boot.log"
        kill $SPRING_PID 2>/dev/null
        exit 1
    fi
    echo -n "."
    sleep 2
done

# Step 6: 간단한 API 테스트
echo -e "\n${BLUE}🧪 Step 6: API 동작 확인${NC}"
echo "포인트 충전 API 테스트..."
CHARGE_RESULT=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8083/api/users/balance/charge \
  -H "Content-Type: application/json" \
  -d '{"userId": 1001, "amount": 10000}')

if [ "$CHARGE_RESULT" = "200" ]; then
    echo -e "${GREEN}✅ 포인트 충전 API 정상 동작${NC}"
else
    echo -e "${RED}❌ 포인트 충전 API 오류 (HTTP: $CHARGE_RESULT)${NC}"
fi

# Step 7: K6 테스트 선택 메뉴
echo -e "\n${BLUE}🚀 Step 7: K6 부하 테스트 시작${NC}"
echo "실행할 테스트를 선택하세요:"
echo "1) 포인트 충전 - Load Test (추천)"
echo "2) 포인트 충전 - Stress Test"
echo "3) 주문 처리 - Load Test"
echo "4) E2E 쇼핑 시나리오"
echo "5) 동시성 테스트 (데드락/레이스)"
echo "6) 선착순 쿠폰 발급 테스트"
echo "7) 모든 테스트 순차 실행"
echo "0) 테스트 없이 환경만 유지"

read -p "선택 (0-7): " choice

case $choice in
    1)
        echo -e "\n${GREEN}📊 포인트 충전 Load Test 실행${NC}"
        k6 run --out influxdb=http://localhost:8086/k6 -e BASE_URL=http://localhost:8083 k6-tests/scenarios/balance/super-quick-test.js
        ;;
    2)
        echo -e "\n${GREEN}💥 포인트 충전 Stress Test 실행${NC}"
        k6 run --out influxdb=http://localhost:8086/k6 -e BASE_URL=http://localhost:8083 k6-tests/scenarios/balance/charge-stress-test.js
        ;;
    3)
        echo -e "\n${GREEN}🛒 주문 처리 Load Test 실행${NC}"
        k6 run --out influxdb=http://localhost:8086/k6 -e BASE_URL=http://localhost:8083 k6-tests/scenarios/order/order-load-test.js
        ;;
    4)
        echo -e "\n${GREEN}🔄 E2E 쇼핑 시나리오 실행${NC}"
        k6 run --out influxdb=http://localhost:8086/k6 -e BASE_URL=http://localhost:8083 k6-tests/scenarios/integrated/e2e-shopping-scenario.js
        ;;
    5)
        echo -e "\n${GREEN}⚠️ 동시성 테스트 실행${NC}"
        k6 run --out influxdb=http://localhost:8086/k6 -e BASE_URL=http://localhost:8083 k6-tests/scenarios/integrated/concurrent-operations-test.js
        ;;
    6)
        echo -e "\n${GREEN}🎫 선착순 쿠폰 발급 테스트 실행${NC}"
        k6 run --out influxdb=http://localhost:8086/k6 -e BASE_URL=http://localhost:8083 k6-tests/scenarios/coupon/coupon-fcfs-test.js
        ;;
    7)
        echo -e "\n${GREEN}🔄 모든 테스트 순차 실행${NC}"
        echo "1. 포인트 충전 Load Test"
        k6 run --out influxdb=http://localhost:8086/k6 -e BASE_URL=http://localhost:8083 k6-tests/scenarios/balance/charge-moderate-test.js
        
        echo -e "\n${YELLOW}재고 초기화를 위해 애플리케이션 재시작 중...${NC}"
        kill $SPRING_PID 2>/dev/null
        sleep 5
        ./gradlew bootRun > spring-boot.log 2>&1 &
        SPRING_PID=$!
        echo "Spring Boot PID: $SPRING_PID"
        
        # 애플리케이션 준비 대기
        for i in {1..30}; do
            if curl -f http://localhost:8083/actuator/health >/dev/null 2>&1; then
                echo -e "${GREEN}✅ 애플리케이션 재시작 완료${NC}"
                break
            fi
            if [ $i -eq 30 ]; then
                echo -e "${RED}❌ 애플리케이션 재시작 실패${NC}"
                exit 1
            fi
            echo -n "."
            sleep 2
        done
        
        # 재고 상태 확인
        echo -e "${BLUE}📊 재고 상태 확인${NC}"
        docker exec $(docker-compose ps -q mysql) mysql -uapplication -papplication -e "
        USE ecommerce;
        SELECT product_id, name, stock FROM products WHERE stock > 0 LIMIT 5;
        " 2>/dev/null || echo "재고 확인 실패"
        
        echo -e "\n2. 주문 처리 Load Test"
        k6 run --out influxdb=http://localhost:8086/k6 -e BASE_URL=http://localhost:8083 k6-tests/scenarios/order/order-load-test.js
        
        echo -e "\n${YELLOW}재고 초기화를 위해 애플리케이션 재시작 중...${NC}"
        kill $SPRING_PID 2>/dev/null
        sleep 5
        ./gradlew bootRun > spring-boot.log 2>&1 &
        SPRING_PID=$!
        echo "Spring Boot PID: $SPRING_PID"
        
        # 애플리케이션 준비 대기
        for i in {1..30}; do
            if curl -f http://localhost:8083/actuator/health >/dev/null 2>&1; then
                echo -e "${GREEN}✅ 애플리케이션 재시작 완료${NC}"
                break
            fi
            if [ $i -eq 30 ]; then
                echo -e "${RED}❌ 애플리케이션 재시작 실패${NC}"
                exit 1
            fi
            echo -n "."
            sleep 2
        done
        
        # 재고 상태 확인
        echo -e "${BLUE}📊 재고 상태 확인${NC}"
        docker exec $(docker-compose ps -q mysql) mysql -uapplication -papplication -e "
        USE ecommerce;
        SELECT product_id, name, stock FROM products WHERE stock > 0 LIMIT 5;
        " 2>/dev/null || echo "재고 확인 실패"
        
        echo -e "\n3. E2E 쇼핑 시나리오"
        k6 run --out influxdb=http://localhost:8086/k6 -e BASE_URL=http://localhost:8083 k6-tests/scenarios/integrated/e2e-shopping-scenario.js
        ;;
    0)
        echo -e "\n${GREEN}환경이 준비되었습니다!${NC}"
        echo "Spring Boot 애플리케이션: http://localhost:8083"
        echo "수동으로 K6 테스트를 실행하려면: ./run-k6-tests.sh"
        ;;
    *)
        echo -e "${RED}잘못된 선택입니다${NC}"
        ;;
esac

echo -e "\n${BLUE}📋 환경 정보${NC}"
echo "- MySQL: localhost:3306 (application/application)"
echo "- Redis: localhost:6379"
echo "- Kafka: localhost:9092"
echo "- Spring Boot: http://localhost:8083"
echo "- 로그: tail -f spring-boot.log"
echo ""
echo "정리 명령어:"
echo "- 애플리케이션 종료: kill $SPRING_PID"
echo "- Docker 정리: docker-compose down"

# 테스트 완료 후에도 애플리케이션 유지
if [ "$choice" != "0" ]; then
    echo -e "\n${YELLOW}테스트가 완료되었습니다. 애플리케이션은 계속 실행 중입니다.${NC}"
    echo "종료하려면 Ctrl+C를 누르거나 'kill $SPRING_PID'를 실행하세요."
fi