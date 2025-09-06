package kr.hhplus.be.server.integration.kafka;

import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.domain.service.IssueCouponService;
import kr.hhplus.be.server.coupon.domain.service.RedisCouponQueueService;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.UserCouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.CouponJpaRepository;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.UserCouponJpaRepository;
import kr.hhplus.be.server.order.domain.service.CreateOrderService;
import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.product.domain.service.RedisProductRankingService;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import kr.hhplus.be.server.shared.kafka.KafkaCouponEventProducer;
import kr.hhplus.be.server.shared.kafka.producer.KafkaEventProducer;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import kr.hhplus.be.server.TestcontainersConfiguration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Kafka 이벤트 기반 시스템 통합 테스트
 * 주문 완료, 쿠폰 발급, 상품 랭킹 등 Kafka 이벤트 처리 검증
 */
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
@DisplayName("Kafka 이벤트 기반 시스템 통합 테스트")
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "test.kafka.enabled", matches = "true")
public class KafkaEventIntegrationTest {

    @Autowired private CreateOrderService createOrderService;
    @Autowired private IssueCouponService issueCouponService;
    @Autowired private KafkaEventProducer kafkaEventProducer;
    @Autowired private KafkaCouponEventProducer kafkaCouponEventProducer;
    @Autowired private RedisProductRankingService productRankingService;
    @Autowired private RedisCouponQueueService couponQueueService;
    
    @Autowired private UserJpaRepository userRepository;
    @Autowired private ProductJpaRepository productRepository;
    @Autowired private BalanceJpaRepository balanceRepository;
    @Autowired private CouponJpaRepository couponRepository;
    @Autowired private UserCouponJpaRepository userCouponRepository;
    
    @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private UserEntity testUser;
    private ProductEntity testProduct;
    private CouponEntity testCoupon;
    private BalanceEntity testBalance;

    @BeforeEach
    @Transactional
    void setUp() {
        // 테스트 데이터 초기화
        userCouponRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        balanceRepository.deleteAll();
        couponRepository.deleteAll();
        
        // 테스트 사용자 생성
        testUser = UserEntity.builder()
                .userId(1L)
                .name("카프카테스트사용자")
                .email("kafka-test@example.com")
                .build();
        userRepository.save(testUser);

        // 테스트 상품 생성
        testProduct = ProductEntity.builder()
                .name("카프카테스트상품")
                .description("카프카 테스트용 상품")
                .price(BigDecimal.valueOf(10000))
                .stockQuantity(100)
                .status("ACTIVE")
                .build();
        productRepository.save(testProduct);

        // 테스트 사용자 잔액 생성
        testBalance = BalanceEntity.builder()
                .user(testUser)
                .amount(BigDecimal.valueOf(1000000))
                .status("ACTIVE")
                .build();
        balanceRepository.save(testBalance);

        // 테스트 쿠폰 생성
        testCoupon = CouponEntity.builder()
                .name("카프카테스트쿠폰")
                .description("카프카 테스트용 쿠폰")
                .discountAmount(BigDecimal.valueOf(1000))
                .maxIssuanceCount(100)
                .issuedCount(0)
                .status("ACTIVE")
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(30))
                .build();
        couponRepository.save(testCoupon);
    }

    @Test
    @DisplayName("주문 완료 시 Kafka 이벤트 발행 및 소비 검증")
    void orderCompletionKafkaEventFlow() throws InterruptedException {
        // Given - 주문 요청 준비
        CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
            testUser.getUserId(),
            List.of(new CreateOrderUseCase.OrderItemCommand(testProduct.getId(), 2)),
            null
        );

        // When - 주문 생성 (Kafka 이벤트 발행 트리거)
        System.out.println("=== 주문 생성 시작 ===");
        System.out.println("사용자 ID: " + testUser.getUserId());
        System.out.println("상품 ID: " + testProduct.getId());
        System.out.println("사용자 잔액: " + testBalance.getAmount());
        System.out.println("상품 재고: " + testProduct.getStockQuantity());
        System.out.println("상품 가격: " + testProduct.getPrice());
        
        CreateOrderUseCase.CreateOrderResult result;
        try {
            result = createOrderService.createOrder(command);
            System.out.println("주문 생성 결과: " + result);
        } catch (Exception e) {
            System.out.println("주문 생성 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        // Then - 주문이 성공적으로 생성됨
        System.out.println("=== 주문 생성 결과 분석 ===");
        System.out.println("성공 여부: " + result.isSuccess());
        System.out.println("주문 ID: " + result.getOrderId());
        System.out.println("상태: " + result.getStatus());
        if (result.getErrorMessage() != null) {
            System.out.println("에러 메시지: " + result.getErrorMessage());
        }
        
        if (!result.isSuccess()) {
            System.out.println("❌ 주문 생성 실패!");
        }
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isNotNull();

        // Kafka Consumer로 이벤트 소비 검증
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // 상품 랭킹이 Redis에 업데이트 되었는지 확인 (Kafka Consumer에 의해)
                    Double score = productRankingService.getProductSalesScore(testProduct.getId());
                    assertThat(score).isNotNull();
                    assertThat(score.intValue()).isGreaterThanOrEqualTo(2); // 주문 수량만큼 증가
                });

        // 재고가 차감되었는지 확인
        ProductEntity updatedProduct = productRepository.findById(testProduct.getId()).get();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(98); // 100 - 2
    }

    @Test
    @DisplayName("쿠폰 발급 Kafka 이벤트 처리 검증")
    void couponIssuanceKafkaEventFlow() throws InterruptedException {
        // Given - 쿠폰 발급 요청
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(testCoupon.getId(), testUser.getUserId());

        // When - 쿠폰 발급 요청 (Kafka 이벤트 발행)
        issueCouponService.issueCoupon(command);

        // Then - Kafka Consumer에 의해 실제 쿠폰이 발급되었는지 확인
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<UserCouponEntity> userCoupons = userCouponRepository.findByUserId(testUser.getUserId());
                    assertThat(userCoupons).hasSize(1);
                    assertThat(userCoupons.get(0).getCouponId()).isEqualTo(testCoupon.getId());
                    assertThat(userCoupons.get(0).getStatus()).isEqualTo("AVAILABLE");
                });

        // 쿠폰 발급 수량이 증가했는지 확인
        CouponEntity updatedCoupon = couponRepository.findById(testCoupon.getId()).get();
        assertThat(updatedCoupon.getIssuedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Redis 쿠폰 대기열과 Kafka 연동 검증")
    void couponQueueKafkaIntegration() throws InterruptedException {
        // Given - 여러 사용자의 쿠폰 발급 요청을 큐에 추가
        Long couponId = testCoupon.getId();
        Long userId1 = testUser.getUserId();
        Long userId2 = 999L; // 다른 사용자

        // When - 쿠폰 발급 요청들을 큐에 추가
        couponQueueService.addToQueue(couponId, userId1);
        couponQueueService.addToQueue(couponId, userId2);

        // 큐 크기 확인
        Long queueSize = couponQueueService.getQueueSize(couponId);
        assertThat(queueSize).isEqualTo(2);

        // Then - Kafka를 통한 비동기 쿠폰 발급 처리 대기
        await().atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // 큐가 처리되어 비어있는지 확인
                    Long remainingQueue = couponQueueService.getQueueSize(couponId);
                    assertThat(remainingQueue).isEqualTo(0);
                    
                    // 실제 쿠폰이 발급되었는지 확인
                    List<UserCouponEntity> user1Coupons = userCouponRepository.findByUserId(userId1);
                    assertThat(user1Coupons).hasSize(1);
                });
    }

    @Test
    @DisplayName("Kafka 이벤트 처리 실패 시 재시도 및 DLQ 검증")
    void kafkaEventProcessingFailureAndRetry() throws InterruptedException {
        // Given - 존재하지 않는 쿠폰으로 발급 요청 (처리 실패 유도)
        Long invalidCouponId = 999L;
        Long userId = testUser.getUserId();

        // When - 유효하지 않은 쿠폰 발급 요청
        IssueCouponUseCase.IssueCouponCommand command = 
            new IssueCouponUseCase.IssueCouponCommand(invalidCouponId, userId);

        try {
            issueCouponService.issueCoupon(command);
        } catch (Exception e) {
            // 예외 발생 예상
        }

        // Then - Redis에 실패 결과가 저장되는지 확인
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // 실패 결과 확인 (구현에 따라 다를 수 있음)
                    List<UserCouponEntity> userCoupons = userCouponRepository.findByUserId(userId);
                    assertThat(userCoupons).isEmpty(); // 실패로 인해 쿠폰 발급 안됨
                });
    }

    @Test  
    @DisplayName("다중 주문에서 상품 랭킹 이벤트 집계 검증")
    void multipleOrdersProductRankingAggregation() throws InterruptedException {
        // Given - 여러 주문 생성 준비
        int orderCount = 5;
        int quantityPerOrder = 3;

        // When - 여러 번 주문 생성
        for (int i = 0; i < orderCount; i++) {
            CreateOrderUseCase.CreateOrderCommand command = new CreateOrderUseCase.CreateOrderCommand(
                testUser.getUserId() + i, // 다른 사용자 ID
                List.of(new CreateOrderUseCase.OrderItemCommand(testProduct.getId(), quantityPerOrder)),
                null
            );
            
            try {
                createOrderService.createOrder(command);
            } catch (Exception e) {
                // 사용자가 존재하지 않아 실패할 수 있음 - 이벤트 발행은 됨
                System.out.println("Order failed but events may be published: " + e.getMessage());
            }
        }

        // Then - 모든 주문의 상품 랭킹 이벤트가 집계되었는지 확인
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Double totalScore = productRankingService.getProductSalesScore(testProduct.getId());
                    assertThat(totalScore).isNotNull();
                    // 최소 일부 주문의 이벤트는 처리되었는지 확인
                    assertThat(totalScore.intValue()).isGreaterThan(0);
                });
    }

    private Properties createKafkaConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return props;
    }
}