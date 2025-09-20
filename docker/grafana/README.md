# K6 + Grafana 시각화 가이드

## 시작하기

### 1. 서비스 시작
```bash
./run-k6-with-grafana.sh
```

### 2. Grafana 접속
- URL: http://localhost:3000
- ID: admin
- PW: admin123

### 3. 대시보드 확인
- 자동으로 "K6 Load Testing Dashboard"가 생성됩니다
- K6 Performance Tests 폴더에서 확인 가능

## 대시보드 구성

### 메트릭 패널
1. **Virtual Users**: 동시 사용자 수 추이
2. **Requests Per Second**: 초당 요청 처리량
3. **Response Time**: P95, P99, 평균 응답 시간
4. **Error Rate**: 에러율 추이
5. **Order Success/Failure Rate**: 주문 성공/실패율
6. **Order Processing Duration**: 주문 처리 시간

## K6 테스트 실행 방법

### 기본 테스트 실행
```bash
# 메뉴에서 선택
./run-k6-with-grafana.sh

# 특정 테스트 직접 실행
./run-k6-with-grafana.sh k6-tests/scenarios/order/order-load-test-influx.js
```

### 커스텀 테스트 실행
```bash
k6 run --out influxdb=http://localhost:8086/k6 your-test.js
```

## 데이터 보존 기간
- InfluxDB는 기본적으로 모든 데이터를 보존합니다
- 필요시 retention policy 설정 가능

## 트러블슈팅

### Grafana 접속 불가
```bash
docker-compose logs grafana
docker-compose restart grafana
```

### InfluxDB 연결 실패
```bash
docker-compose logs influxdb
docker-compose exec influxdb influx -execute "SHOW DATABASES"
```

### 대시보드가 보이지 않는 경우
1. Grafana 재시작: `docker-compose restart grafana`
2. 수동 import: Configuration > Data Sources > InfluxDB 확인

## 서비스 종료
```bash
# 서비스만 중지
docker-compose stop influxdb grafana

# 완전 제거
docker-compose down

# 데이터까지 제거
docker-compose down -v
```