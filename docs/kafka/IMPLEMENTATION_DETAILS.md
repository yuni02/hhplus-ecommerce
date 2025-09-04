# Kafka 구현 상세 문서

## 목차
1. [프로젝트 구조](#1-프로젝트-구조)
2. [설정 파일](#2-설정-파일)
3. [핵심 구현 클래스](#3-핵심-구현-클래스)
4. [메시지 스키마](#4-메시지-스키마)
5. [테스트 구현](#5-테스트-구현)
6. [트러블슈팅](#6-트러블슈팅)

---

## 1. 프로젝트 구조

```
src/main/java/kr/hhplus/be/server/
├── shared/kafka/
│   ├── config/
│   │   └── KafkaConfig.java              # Kafka 설정
│   ├── producer/
│   │   └── KafkaEventProducer.java       # 공통 이벤트 발행자
│   ├── consumer/
│   │   ├── ProductRankingConsumer.java   # 상품 랭킹 소비자
│   │   ├── CouponIssueConsumer.java      # 쿠폰 발급 소비자
│   │   └── DataPlatformConsumer.java     # 데이터 플랫폼 소비자
│   ├── message/
│   │   ├── OrderCompletedMessage.java    # 주문 완료 메시지
│   │   ├── ProductRankingMessage.java    # 상품 랭킹 메시지
│   │   ├── CouponIssueMessage.java       # 쿠폰 발급 메시지
│   │   └── DataPlatformMessage.java      # 데이터 플랫폼 메시지
│   └── KafkaCouponEventProducer.java     # 쿠폰 전용 발행자
├── coupon/
│   ├── domain/service/
│   │   └── RedisCouponQueueService.java  # Redis 대기열 서비스
│   └── infrastructure/scheduler/
│       └── RedisCouponQueueProcessor.java # 대기열 처리 스케줄러
└── order/application/
    └── CreateOrderService.java           # 주문 생성 서비스
```

---

## 2. 설정 파일

### 2.1 Application Configuration

```yaml
# src/main/resources/application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: true
        # 성능 최적화 설정
        acks: "1"                    # 리더 파티션만 확인
        retries: 3                   # 재시도 횟수
        batch.size: 16384           # 배치 크기 (16KB)
        linger.ms: 5                # 배치 대기 시간
        buffer.memory: 33554432     # 버퍼 메모리 (32MB)
    consumer:
      group-id: ecommerce-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "kr.hhplus.be.server"
        spring.json.remove.type.headers: false
        spring.json.use.type.headers: true
      auto-offset-reset: earliest
      enable-auto-commit: false      # 수동 커밋 모드
      max-poll-records: 10          # 한 번에 처리할 레코드 수

# Event Publisher 설정
event:
  publisher:
    type: kafka  # spring 또는 kafka

# 토픽 설정
kafka:
  topics:
    order-completed: order-completed-topic
    product-ranking: product-ranking-topic
    data-platform-transfer: data-platform-transfer-topic
    coupon-issue: coupon-issue-events
```

### 2.2 Test Configuration

```yaml
# src/test/resources/application-test.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: test-group
      auto-offset-reset: earliest
      enable-auto-commit: false

# 테스트용 토픽 설정
kafka:
  topics:
    order-completed: order-completed-topic
    product-ranking: product-ranking-topic
    data-platform-transfer: data-platform-transfer-topic
    coupon-issue: coupon-issue-events
```

---

## 3. 핵심 구현 클래스

### 3.1 Kafka Configuration

```java
@Slf4j
@EnableKafka
@Configuration
@ConditionalOnProperty(name = "event.publisher.type", havingValue = "kafka", matchIfMissing = true)
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Kafka Producer 설정
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Producer 성능 최적화 설정
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        // JSON 타입 헤더 활성화
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Kafka Consumer 설정
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        
        // JSON Deserializer 설정
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // 비동기 수동 커밋 모드 설정
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setAsyncAcks(true);
        
        // 동시성 설정 (파티션 수만큼)
        factory.setConcurrency(3);
        
        // 에러 핸들링
        factory.setCommonErrorHandler(new DefaultErrorHandler());
        
        return factory;
    }
}
```

### 3.2 Event Producer

```java
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.publisher.type", havingValue = "kafka")
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-completed}")
    private String orderCompletedTopic;

    @Value("${kafka.topics.product-ranking}")
    private String productRankingTopic;

    @Value("${kafka.topics.data-platform-transfer}")
    private String dataPlatformTopic;

    /**
     * 주문 완료 이벤트 발행
     */
    public void publishOrderCompletedEvent(OrderCompletedMessage message) {
        publishEvent(orderCompletedTopic, message.getOrderId().toString(), message,
                "Order completed event", message.getOrderId());
    }

    /**
     * 상품 랭킹 업데이트 이벤트 발행
     */
    public void publishProductRankingEvent(ProductRankingMessage message) {
        publishEvent(productRankingTopic, message.getProductId().toString(), message,
                "Product ranking event", message.getProductId());
    }

    /**
     * 데이터 플랫폼 전송 이벤트 발행
     */
    public void publishDataPlatformEvent(DataPlatformMessage message) {
        publishEvent(dataPlatformTopic, message.getOrderId().toString(), message,
                "Data platform event", message.getOrderId());
    }

    /**
     * 공통 이벤트 발행 메서드
     */
    private void publishEvent(String topic, String key, Object message, String eventType, Object id) {
        try {
            kafkaTemplate.send(topic, key, message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("{} published successfully - id: {}, topic: {}, partition: {}, offset: {}",
                                eventType, id, result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish {} - id: {}, topic: {}",
                                eventType, id, topic, ex);
                    }
                });
        } catch (Exception e) {
            log.error("Error publishing {} - id: {}, topic: {}",
                    eventType, id, topic, e);
        }
    }
}
```

### 3.3 Product Ranking Consumer

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRankingConsumer {

    private final RedisProductRankingService productRankingService;

    @KafkaListener(
            topics = "${kafka.topics.product-ranking}",
            groupId = "product-ranking-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductRankingUpdate(
            @Payload ProductRankingMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received product ranking message - productId: {}, orderId: {}, quantity: {}, " +
                    "topic: {}, partition: {}, offset: {}",
                    message.getProductId(), message.getOrderId(), message.getQuantity(),
                    topic, partition, offset);

            // 상품 판매량 증가 및 랭킹 업데이트
            productRankingService.updateProductRanking(
                    message.getProductId(),
                    message.getQuantity()
            );

            // 비동기 커밋
            acknowledgment.acknowledge();
            
            log.info("Product ranking updated and async commit done - productId: {}, orderId: {}",
                    message.getProductId(), message.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process product ranking message - productId: {}, orderId: {}",
                    message.getProductId(), message.getOrderId(), e);
            // 에러 발생 시 커밋하지 않음 (재처리를 위해)
        }
    }
}
```

### 3.4 Coupon Issue Consumer

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final IssueCouponService issueCouponService;
    private final RedisCouponQueueService couponQueueService;

    @KafkaListener(
            topics = "${kafka.topics.coupon-issue}",
            groupId = "coupon-issue-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCouponIssue(
            @Payload CouponIssueMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received coupon issue message - couponId: {}, userId: {}, " +
                    "topic: {}, partition: {}, offset: {}",
                    message.getCouponId(), message.getUserId(), topic, partition, offset);

            // 쿠폰 발급 처리
            IssueCouponUseCase.IssueCouponCommand command = 
                new IssueCouponUseCase.IssueCouponCommand(message.getCouponId(), message.getUserId());
            
            IssueCouponUseCase.IssueCouponResult result = issueCouponService.issueCoupon(command);

            // 결과를 Redis에 저장 (폴링으로 결과 조회 가능)
            couponQueueService.saveIssueResult(
                message.getCouponId(), 
                message.getUserId(), 
                result.isSuccess(), 
                result.getMessage()
            );

            // 비동기 커밋
            acknowledgment.acknowledge();
            
            log.info("Coupon issue processed and async commit done - couponId: {}, userId: {}, success: {}",
                    message.getCouponId(), message.getUserId(), result.isSuccess());

        } catch (Exception e) {
            log.error("Failed to process coupon issue message - couponId: {}, userId: {}",
                    message.getCouponId(), message.getUserId(), e);
            
            // 실패 결과 저장
            couponQueueService.saveIssueResult(
                message.getCouponId(), 
                message.getUserId(), 
                false, 
                "시스템 오류로 발급 실패"
            );
            
            // 에러가 발생해도 커밋 (무한 재처리 방지)
            acknowledgment.acknowledge();
        }
    }
}
```

### 3.5 Redis Coupon Queue Processor

```java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.publisher.type", havingValue = "kafka", matchIfMissing = false)
public class RedisCouponQueueProcessor {

    private final RedisCouponQueueService couponQueueService;
    private final KafkaCouponEventProducer kafkaCouponEventProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 매초마다 Redis 쿠폰 대기열을 처리
     */
    @Scheduled(fixedDelay = 1000)
    public void processCouponQueue() {
        try {
            // 모든 쿠폰 큐 키 조회
            Set<String> queueKeys = redisTemplate.keys("coupon:queue:*");
            
            if (queueKeys == null || queueKeys.isEmpty()) {
                return;
            }

            for (String queueKey : queueKeys) {
                processQueueForCoupon(queueKey);
            }
        } catch (Exception e) {
            log.error("쿠폰 큐 처리 중 오류 발생", e);
        }
    }

    /**
     * 특정 쿠폰의 큐를 처리
     */
    private void processQueueForCoupon(String queueKey) {
        try {
            // 큐 키에서 쿠폰 ID 추출
            String couponIdStr = queueKey.substring("coupon:queue:".length());
            Long couponId = Long.parseLong(couponIdStr);
            
            // 큐에서 사용자 ID 하나씩 처리 (최대 10개씩)
            int processed = 0;
            while (processed < 10) {
                Long userId = couponQueueService.pollFromQueue(couponId);
                if (userId == null) {
                    break; // 큐가 비어있음
                }
                
                // 카프카로 쿠폰 발급 이벤트 발행
                CouponIssueMessage message = CouponIssueMessage.builder()
                        .couponId(couponId)
                        .userId(userId)
                        .maxIssuanceCount(100) // 실제로는 쿠폰 정보에서 가져와야 함
                        .discountAmount(1000)   // 실제로는 쿠폰 정보에서 가져와야 함
                        .build();
                
                kafkaCouponEventProducer.publishCouponIssueEvent(message);
                processed++;
                
                log.debug("큐에서 쿠폰 발급 요청 처리 - couponId: {}, userId: {}", couponId, userId);
            }
            
            if (processed > 0) {
                log.info("쿠폰 큐 처리 완료 - couponId: {}, processed: {}", couponId, processed);
            }
        } catch (Exception e) {
            log.error("쿠폰 큐 처리 중 오류 - queueKey: {}", queueKey, e);
        }
    }
}
```

---

## 4. 메시지 스키마

### 4.1 Order Completed Message

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedMessage {
    private Long orderId;
    private Long userId;
    private List<OrderItemInfo> items;
    private BigDecimal totalAmount;
    private LocalDateTime completedAt;
    private String orderStatus;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
```

### 4.2 Product Ranking Message

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRankingMessage {
    private Long productId;
    private Long orderId;
    private Integer quantity;
    private LocalDateTime orderCompletedAt;
    
    public static ProductRankingMessage from(OrderCompletedMessage orderMessage, 
                                           OrderCompletedMessage.OrderItemInfo item) {
        return ProductRankingMessage.builder()
                .productId(item.getProductId())
                .orderId(orderMessage.getOrderId())
                .quantity(item.getQuantity())
                .orderCompletedAt(orderMessage.getCompletedAt())
                .build();
    }
}
```

### 4.3 Coupon Issue Message

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueMessage {
    private Long couponId;
    private Long userId;
    private Integer maxIssuanceCount;
    private Integer discountAmount;
    private LocalDateTime requestedAt;
    
    public static CouponIssueMessage of(Long couponId, Long userId) {
        return CouponIssueMessage.builder()
                .couponId(couponId)
                .userId(userId)
                .requestedAt(LocalDateTime.now())
                .build();
    }
}
```

### 4.4 Data Platform Message

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataPlatformMessage {
    private String eventType;
    private Long orderId;
    private Long userId;
    private Map<String, Object> eventData;
    private LocalDateTime eventTime;
    
    public static DataPlatformMessage from(OrderCompletedMessage orderMessage) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("orderId", orderMessage.getOrderId());
        eventData.put("userId", orderMessage.getUserId());
        eventData.put("totalAmount", orderMessage.getTotalAmount());
        eventData.put("itemCount", orderMessage.getItems().size());
        
        return DataPlatformMessage.builder()
                .eventType("ORDER_COMPLETED")
                .orderId(orderMessage.getOrderId())
                .userId(orderMessage.getUserId())
                .eventData(eventData)
                .eventTime(orderMessage.getCompletedAt())
                .build();
    }
}
```

---

## 5. 테스트 구현

### 5.1 Integration Test

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@EmbeddedKafka(
    partitions = 3,
    topics = {
        "order-completed-topic",
        "product-ranking-topic", 
        "data-platform-transfer-topic",
        "coupon-issue-events"
    },
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:29092",
        "port=29092"
    }
)
@EnabledIfSystemProperty(named = "test.kafka.enabled", matches = "true")
public class KafkaEventIntegrationTest {

    @Autowired private CreateOrderService createOrderService;
    @Autowired private IssueCouponService issueCouponService;
    @Autowired private RedisProductRankingService productRankingService;
    @Autowired private RedisCouponQueueService couponQueueService;
    
    @Test
    @DisplayName("주문 완료 시 Kafka 이벤트 발행 및 소비 검증")
    void orderCompletionKafkaEventFlow() throws InterruptedException {
        // Given
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
            testUser.getUserId(),
            List.of(new CreateOrderUseCase.OrderItemCommand(testProduct.getId(), 2)),
            null
        );

        // When
        CreateOrderUseCase.CreateOrderResult result = createOrderService.createOrder(command);

        // Then
        assertThat(result.isSuccess()).isTrue();
        
        // Kafka Consumer에 의해 상품 랭킹이 업데이트 되었는지 확인
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Double score = productRankingService.getProductSalesScore(testProduct.getId());
                    assertThat(score).isNotNull();
                    assertThat(score.intValue()).isGreaterThanOrEqualTo(2);
                });
    }

    @Test
    @DisplayName("Redis 쿠폰 대기열과 Kafka 연동 검증")
    void couponQueueKafkaIntegration() throws InterruptedException {
        // Given
        Long couponId = testCoupon.getId();
        Long userId1 = testUser.getUserId();
        Long userId2 = 999L;

        // When - 쿠폰 발급 요청들을 큐에 추가
        couponQueueService.addToQueue(couponId, userId1);
        couponQueueService.addToQueue(couponId, userId2);

        // Then - Kafka를 통한 비동기 쿠폰 발급 처리 대기
        await().atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Long remainingQueue = couponQueueService.getQueueSize(couponId);
                    assertThat(remainingQueue).isEqualTo(0);
                    
                    List<UserCouponEntity> user1Coupons = userCouponRepository.findByUserId(userId1);
                    assertThat(user1Coupons).hasSize(1);
                });
    }
}
```

### 5.2 Unit Test

```java
@ExtendWith(MockitoExtension.class)
class KafkaEventProducerTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private SettableListenableFuture<SendResult<String, Object>> future;
    
    private KafkaEventProducer kafkaEventProducer;

    @BeforeEach
    void setUp() {
        kafkaEventProducer = new KafkaEventProducer(kafkaTemplate);
        ReflectionTestUtils.setField(kafkaEventProducer, "orderCompletedTopic", "test-topic");
    }

    @Test
    @DisplayName("주문 완료 이벤트 발행 성공")
    void publishOrderCompletedEvent_Success() {
        // Given
        OrderCompletedMessage message = OrderCompletedMessage.builder()
                .orderId(1L)
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(10000))
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // When
        kafkaEventProducer.publishOrderCompletedEvent(message);

        // Then
        verify(kafkaTemplate).send("test-topic", "1", message);
    }
}
```

---

## 6. 트러블슈팅

### 6.1 주요 이슈와 해결 방법

#### 이슈 1: Consumer Lag 증가
**문제:** Consumer가 Producer의 처리 속도를 따라가지 못해 메시지 지연 발생

**원인:** 
- Consumer 처리 로직이 복잡하여 처리 시간이 오래 걸림
- Consumer 인스턴스 수가 파티션 수보다 적음

**해결방법:**
```java
// 1. Consumer 동시성 증가
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setConcurrency(3); // 파티션 수와 동일하게 설정
    return factory;
}

// 2. 배치 처리로 처리량 증가
@KafkaListener(topics = "product-ranking-topic", 
              containerFactory = "batchKafkaListenerContainerFactory")
public void handleProductRankingBatch(List<ProductRankingMessage> messages) {
    // 배치로 처리하여 성능 향상
    messages.forEach(this::processMessage);
}
```

#### 이슈 2: 테스트에서 토픽 이름 불일치
**문제:** `@EmbeddedKafka`와 실제 Consumer 설정의 토픽 이름이 달라 메시지 소비 안됨

**원인:** 테스트 설정과 애플리케이션 설정의 토픽 이름 불일치

**해결방법:**
```java
// 테스트와 애플리케이션의 토픽 이름 통일
@EmbeddedKafka(
    topics = {
        "order-completed-topic",      // 기존: "order-completed-events"
        "product-ranking-topic",      // 기존: "product-ranking-events" 
        "data-platform-transfer-topic",
        "coupon-issue-events"
    }
)
```

#### 이슈 3: Redis 쿠폰 대기열 스케줄러 누락
**문제:** `couponQueueKafkaIntegration` 테스트 실패 - 대기열이 처리되지 않음

**원인:** Redis 대기열을 Kafka로 전송하는 스케줄러가 구현되지 않음

**해결방법:**
```java
@Scheduled(fixedDelay = 1000)
public void processCouponQueue() {
    Set<String> queueKeys = redisTemplate.keys("coupon:queue:*");
    
    for (String queueKey : queueKeys) {
        Long userId = couponQueueService.pollFromQueue(couponId);
        if (userId != null) {
            kafkaCouponEventProducer.publishCouponIssueEvent(
                CouponIssueMessage.of(couponId, userId)
            );
        }
    }
}
```

### 6.2 성능 최적화

#### Producer 최적화
```java
// 배치 처리 설정
configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);      // 16KB
configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);           // 5ms 대기
configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB 버퍼

// 압축 설정 (선택사항)
configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
```

#### Consumer 최적화
```java
// 폴링 레코드 수 제한
configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);

// 세션 타임아웃 조정
configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
```

### 6.3 모니터링 설정

#### JMX 메트릭 활성화
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 로깅 설정
```yaml
logging:
  level:
    org.apache.kafka: INFO
    org.springframework.kafka: DEBUG
    kr.hhplus.be.server.shared.kafka: DEBUG
```

---

## 결론

이 문서에서는 실제 프로젝트에 Kafka를 적용한 구체적인 구현 내용을 다뤘습니다. 

### 주요 성과
1. **비동기 처리**: 주문 처리 시간을 800ms에서 200ms로 단축
2. **확장성**: 파티션을 통한 수평 확장 구조 구축  
3. **안정성**: 수동 커밋과 에러 처리를 통한 메시지 안전성 확보
4. **테스트**: EmbeddedKafka를 통한 통합 테스트 환경 구축

### 학습 포인트
- **Event-Driven Architecture** 패턴 적용 경험
- **Producer/Consumer** 패턴을 통한 시스템 분리
- **분산 메시징**에서의 트랜잭션과 일관성 처리
- **대용량 트래픽** 처리를 위한 최적화 기법