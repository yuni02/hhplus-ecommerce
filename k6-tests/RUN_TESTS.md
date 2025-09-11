# K6 부하 테스트 실행 가이드

## 🚀 빠른 시작

### 1. K6 설치
```bash
# MacOS
brew install k6

# Windows (Chocolatey)
choco install k6

# Docker
docker pull grafana/k6
```

### 2. 애플리케이션 실행
```bash
# Docker Compose로 DB 실행
docker-compose up -d

# Spring Boot 애플리케이션 실행
./gradlew bootRun
```

## 📋 테스트 실행 명령어

### 포인트 충전 테스트

#### Load Test (정상 부하)
```bash
k6 run k6-tests/scenarios/balance/charge-load-test.js

# 커스텀 설정
k6 run -e BASE_URL=http://localhost:8080 k6-tests/scenarios/balance/charge-load-test.js
```

#### Stress Test (한계 테스트)
```bash
k6 run k6-tests/scenarios/balance/charge-stress-test.js
```

#### Peak Test (급증 테스트)
```bash
k6 run k6-tests/scenarios/balance/charge-peak-test.js
```

### 주문 처리 테스트

#### Load Test
```bash
k6 run k6-tests/scenarios/order/order-load-test.js
```

#### Stress Test
```bash
k6 run k6-tests/scenarios/order/order-stress-test.js
```

#### Peak Test (플래시 세일 시뮬레이션)
```bash
k6 run k6-tests/scenarios/order/order-peak-test.js
```

### 통합 시나리오 테스트

#### E2E 쇼핑 시나리오
```bash
k6 run k6-tests/scenarios/integrated/e2e-shopping-scenario.js
```

#### 동시성 테스트 (데드락/레이스 컨디션 검증)
```bash
k6 run k6-tests/scenarios/integrated/concurrent-operations-test.js
```

#### 내구성 테스트 (장시간 운영)
```bash
k6 run k6-tests/scenarios/integrated/endurance-test.js
```

## 📊 결과 분석

### 실시간 모니터링
```bash
# 콘솔에서 실시간 메트릭 확인
k6 run --out influxdb=http://localhost:8086/k6 k6-tests/scenarios/order/order-load-test.js
```

### JSON 결과 저장
```bash
k6 run --out json=results/test-results.json k6-tests/scenarios/order/order-load-test.js
```

### HTML 리포트 생성
```bash
# 테스트 실행 후 HTML 리포트 생성
k6 run --out json=results.json k6-tests/scenarios/order/order-load-test.js
k6-to-html results.json report.html
```

## 🎯 주요 메트릭 해석

### 핵심 지표

| 메트릭 | 설명 | 목표값 |
|--------|------|--------|
| `http_req_duration` | 요청 응답 시간 | P95 < 500ms |
| `http_req_failed` | 실패한 요청 비율 | < 1% |
| `http_reqs` | 초당 요청 수 (TPS) | API별 상이 |
| `iterations` | 완료된 시나리오 수 | - |
| `vus` | 활성 가상 사용자 수 | - |

### 커스텀 메트릭

| 메트릭 | 설명 | 확인 방법 |
|--------|------|-----------|
| `errors` | 비즈니스 로직 에러율 | Rate 메트릭 |
| `order_duration` | 주문 처리 시간 | Trend 메트릭 |
| `stock_errors` | 재고 부족 에러 | Counter 메트릭 |
| `deadlocks` | 데드락 발생 횟수 | Counter 메트릭 |

## 🔍 결과 분석 예시

### 성공적인 테스트 결과
```
✓ status is 200
✓ response has orderId
✓ http_req_duration..............: p(95)=487.52ms p(99)=892.14ms
✓ http_req_failed................: 0.52% ✓ 52 ✗ 9948
✓ http_reqs......................: 55.2/s
```

### 문제가 있는 테스트 결과
```
✗ http_req_duration..............: p(95)=2847.52ms p(99)=5123.14ms
✗ http_req_failed................: 15.23% ✗ 1523 ✓ 8477
  stock_errors...................: 823
  timeouts.......................: 45
  potential_deadlocks............: 12
```

## 🚨 병목 지점 분석

### 1. 데이터베이스 락 경합
- **증상**: `order_duration` 급증, `timeouts` 증가
- **확인**: 동시 주문 시 응답 시간 증가
- **테스트**: `concurrent-operations-test.js`

### 2. 재고 부족
- **증상**: `stock_errors` 카운터 증가
- **확인**: 400 에러 with "재고 부족" 메시지
- **테스트**: `order-stress-test.js`

### 3. 메모리 누수
- **증상**: `response_time_degradation` 지속 증가
- **확인**: 시간이 지날수록 응답 시간 악화
- **테스트**: `endurance-test.js`

### 4. 커넥션 풀 고갈
- **증상**: `timeouts` 급증, 503 에러
- **확인**: VU 증가 시 급격한 성능 저하
- **테스트**: `order-peak-test.js`

## 📈 성능 튜닝 제안

### 테스트 결과별 조치 사항

1. **응답 시간이 느린 경우**
   - DB 인덱스 확인
   - 쿼리 최적화
   - 캐싱 전략 검토

2. **동시성 문제 발생 시**
   - 락 전략 재검토 (낙관적 vs 비관적)
   - 트랜잭션 범위 최소화
   - 이벤트 기반 비동기 처리 도입

3. **높은 에러율**
   - 서킷 브레이커 패턴 적용
   - 재시도 로직 구현
   - 리소스 풀 크기 조정

## 🔧 트러블슈팅

### 일반적인 문제 해결

#### K6 실행 오류
```bash
# 권한 문제
sudo k6 run test.js

# 모듈 import 오류
k6 run --compatibility-mode=extended test.js
```

#### 타임아웃 문제
```javascript
// 타임아웃 설정 증가
const params = {
    timeout: '30s', // 기본 10s에서 30s로 증가
};
```

#### 메모리 부족
```bash
# 힙 메모리 증가
k6 run --max-redirects=10 --batch=100 test.js
```

## 📝 보고서 작성 템플릿

### 부하 테스트 결과 보고서

```markdown
## 테스트 개요
- 테스트 일시: 2024-XX-XX
- 테스트 환경: Local / Staging / Production
- 테스트 시나리오: [시나리오명]
- 테스트 시간: XX분

## 테스트 결과
- 최대 TPS: XXX
- 평균 응답 시간: XXXms
- P95 응답 시간: XXXms
- 에러율: X.X%

## 병목 지점
1. [발견된 병목 지점]
2. [추가 병목 지점]

## 개선 제안
1. [개선 방안 1]
2. [개선 방안 2]
```

## 🎓 추가 학습 자료

- [K6 공식 문서](https://k6.io/docs/)
- [K6 예제 모음](https://github.com/grafana/k6-example-scenarios)
- [성능 테스트 Best Practices](https://k6.io/docs/testing-guides/)