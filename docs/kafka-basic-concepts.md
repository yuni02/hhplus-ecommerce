# Apache Kafka 기본 개념 및 적용

## 1. Kafka란 무엇인가?

Apache Kafka는 **분산 스트리밍 플랫폼**으로, 대용량의 실시간 데이터 스트림을 안전하게 전송하고 처리할 수 있는 메시지 브로커입니다.

### 왜 대용량 트래픽 서비스에서 Kafka를 사용하는가?

1. **높은 처리량**: 초당 수백만 개의 메시지 처리 가능
2. **내결함성**: 장애 발생 시에도 데이터 손실 없이 서비스 지속 가능
3. **확장성**: 수평적 확장을 통해 처리량 증대 가능
4. **순서 보장**: 파티션 내에서 메시지 순서 보장
5. **비동기 처리**: 시스템 간 결합도를 낮추고 성능 향상

## 2. Kafka 핵심 구성요소

### 2.1 Producer & Consumer

- **Producer**: 메시지를 Kafka 브로커에 발행하는 애플리케이션
- **Consumer**: Kafka 브로커에서 메시지를 소비하는 애플리케이션

```java
// Producer 예제
kafkaTemplate.send(topic, key, message);

// Consumer 예제
@KafkaListener(topics = "order-completed-topic")
public void handleMessage(OrderCompletedMessage message) {
    // 메시지 처리 로직
}
```

### 2.2 Broker

- Kafka 서버 단위
- Producer의 메시지를 받아 디스크에 저장
- Consumer의 요청에 응답하여 메시지 전송

### 2.3 Topic & Partition

- **Topic**: 메시지를 분류하는 단위 (예: `order-completed-topic`)
- **Partition**: Topic을 구성하는 물리적 단위
  - 파티션 내에서 메시지 순서 보장
  - 파티션 수만큼 병렬 처리 가능

```yaml
# 토픽 설정 예제
kafka:
  topics:
    order-completed: order-completed-topic
    product-ranking: product-ranking-topic
    data-platform-transfer: data-platform-transfer-topic
```

### 2.4 Consumer Group

- 하나의 토픽을 여러 애플리케이션이 소비할 수 있도록 하는 단위
- 같은 Consumer Group 내에서는 파티션별로 하나의 Consumer만 처리
- 서로 다른 Consumer Group은 독립적으로 메시지 소비

## 3. 프로젝트 적용 사례

### 3.1 주문 완료 이벤트 처리

```java
// 기존: Spring Event 기반
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleDataPlatformTransfer(OrderCompletedEvent event) {
    // 직접 API 호출 - 외부 서비스 장애 시 영향
    dataPlatformService.sendOrderData(orderData);
}

// 개선: Kafka 기반
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderCompleted(OrderCompletedEvent event) {
    // Kafka로 메시지 발행 - 장애 격리
    kafkaEventProducer.publishDataPlatformTransfer(message);
}
```

### 3.2 장애 격리 및 책임 분리

**Before (Spring Event)**:
- 주문 서비스가 데이터 플랫폼 API 호출 책임
- 외부 서비스 장애 시 주문 서비스 성능 저하
- 재시도 로직을 주문 서비스에서 관리

**After (Kafka)**:
- 주문 서비스는 Kafka에 메시지 발행만 담당
- 데이터 플랫폼 서비스가 Consumer로 메시지 소비
- 외부 서비스 장애와 주문 서비스 완전 분리

### 3.3 순서 보장 전략

```java
// 사용자별 순서 보장
String key = "user-" + message.getUserId();
kafkaTemplate.send(topic, key, message);

// 상품별 순서 보장
String key = "product-" + message.getProductId();
kafkaTemplate.send(topic, key, message);
```

- 동일한 키를 가진 메시지는 같은 파티션에 저장
- 파티션 내에서 순서 보장
- 서로 다른 키는 다른 파티션에서 병렬 처리

## 4. 성능 최적화 방안

### 4.1 파티션 수 설정

```bash
# 토픽 생성 시 파티션 수 지정
kafka-topics --create --topic order-completed-topic --partitions 3 --replication-factor 1
```

- **처리량 향상**: 파티션 수만큼 병렬 처리
- **Consumer 확장**: 파티션 수만큼 Consumer 인스턴스 확장 가능

### 4.2 Producer 최적화

```yaml
spring:
  kafka:
    producer:
      acks: "1"           # 리더 파티션 확인
      retries: 3          # 재시도 횟수
      batch-size: 16384   # 배치 크기
      linger-ms: 5        # 배치 대기 시간
```

### 4.3 Consumer 최적화

```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 10        # 한 번에 처리할 레코드 수
      enable-auto-commit: false   # 수동 커밋으로 정확성 보장
      auto-offset-reset: earliest # 처음부터 읽기
```

## 5. 운영 환경 고려사항

### 5.1 모니터링

- **Kafka UI**: 토픽, 파티션, 컨슈머 그룹 상태 모니터링
- **Consumer Lag**: 처리되지 않은 메시지 수 모니터링
- **처리량 모니터링**: Producer/Consumer 처리량 추적

### 5.2 장애 대응

- **Dead Letter Queue**: 처리 실패 메시지 별도 토픽으로 이동
- **재시도 정책**: 일시적 장애에 대한 재시도 로직
- **Circuit Breaker**: 외부 API 호출 시 장애 전파 방지

## 6. 실습 환경 구성

### 6.1 Docker Compose로 Kafka 실행

```bash
docker-compose up -d zookeeper kafka kafka-ui
```

### 6.2 토픽 생성 및 확인

```bash
# 토픽 생성
docker exec -it kafka kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# 토픽 목록 확인
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# 메시지 발행 (CLI)
docker exec -it kafka kafka-console-producer --topic test-topic --bootstrap-server localhost:9092

# 메시지 소비 (CLI)
docker exec -it kafka kafka-console-consumer --topic test-topic --bootstrap-server localhost:9092 --from-beginning
```

### 6.3 Kafka UI 접속

- URL: http://localhost:8080
- 토픽, 파티션, 메시지 확인 가능

## 7. 애플리케이션 설정 변경

### event.publisher.type 설정으로 전환

```yaml
# application.yml
event:
  publisher:
    type: kafka  # spring 또는 kafka
```

- `spring`: 기존 Spring Event 방식
- `kafka`: Kafka 기반 이벤트 처리

이 설정을 통해 운영 환경에서 점진적으로 Kafka로 전환 가능합니다.

## 결론

Kafka를 도입함으로써:
1. **성능 향상**: 비동기 메시지 처리로 응답 시간 개선
2. **장애 격리**: 외부 서비스 장애가 핵심 비즈니스 로직에 미치는 영향 최소화
3. **확장성**: 수평 확장을 통한 처리량 증대
4. **책임 분리**: 각 서비스가 자신의 책임 영역에만 집중

대용량 트래픽을 처리하는 현대적인 마이크로서비스 아키텍처에서 Kafka는 필수적인 인프라 구성요소입니다.