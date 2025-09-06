package kr.hhplus.be.server.integration.kafka;

import kr.hhplus.be.server.shared.kafka.CouponIssueMessage;
import kr.hhplus.be.server.shared.kafka.KafkaCouponEventProducer;
import kr.hhplus.be.server.coupon.domain.service.KafkaCouponIssueEventHandler;
import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity;
import kr.hhplus.be.server.coupon.infrastructure.persistence.repository.CouponJpaRepository;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;
import kr.hhplus.be.server.TestcontainersConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 간단한 Kafka 이벤트 테스트
 * 핵심 Producer/Consumer 동작 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@EmbeddedKafka(
    partitions = 3,
    topics = {"coupon-issue-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:19092",
        "port=19092"
    }
)
@DisplayName("간단한 Kafka 이벤트 테스트")
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "test.kafka.enabled", matches = "true")
public class SimpleKafkaEventTest {

    @Autowired private KafkaCouponEventProducer producer;
    @Autowired private CouponJpaRepository couponRepository;
    @Autowired private UserJpaRepository userRepository;

    private CouponEntity testCoupon;
    private UserEntity testUser;

    @BeforeEach
    @Transactional
    void setUp() {
        couponRepository.deleteAll();
        userRepository.deleteAll();
        
        testUser = UserEntity.builder()
                .userId(1L)
                .name("테스트사용자")
                .email("test@example.com")
                .build();
        userRepository.save(testUser);

        testCoupon = CouponEntity.builder()
                .name("테스트쿠폰")
                .description("Kafka 테스트용")
                .discountAmount(BigDecimal.valueOf(1000))
                .maxIssuanceCount(10)
                .issuedCount(0)
                .status("ACTIVE")
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(30))
                .build();
        couponRepository.save(testCoupon);
    }

    @Test
    @DisplayName("Kafka Producer/Consumer 기본 동작 검증")
    void kafkaProducerConsumerBasicFlow() throws InterruptedException {
        // Given
        CouponIssueMessage message = CouponIssueMessage.builder()
                .couponId(testCoupon.getId())
                .userId(testUser.getUserId())
                .maxIssuanceCount(testCoupon.getMaxIssuanceCount())
                .discountAmount(testCoupon.getDiscountAmount().intValue())
                .build();

        // When - Kafka로 쿠폰 발급 이벤트 발행
        producer.publishCouponIssueEvent(message);

        // Then - Consumer가 메시지를 처리했는지 확인
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // 쿠폰 발급 수량이 증가했는지 확인
                    CouponEntity updatedCoupon = couponRepository.findById(testCoupon.getId()).get();
                    assertThat(updatedCoupon.getIssuedCount()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Kafka 이벤트 발행 확인")
    void kafkaEventPublishing() {
        // Given
        CouponIssueMessage message = CouponIssueMessage.builder()
                .couponId(testCoupon.getId())
                .userId(testUser.getUserId())
                .maxIssuanceCount(testCoupon.getMaxIssuanceCount())
                .discountAmount(1000)
                .build();

        // When & Then - 예외 없이 발행되는지 확인
        try {
            producer.publishCouponIssueEvent(message);
            // 예외 발생하지 않으면 성공
            assertThat(true).isTrue();
        } catch (Exception e) {
            // 예외 발생 시 실패
            assertThat(e).isNull();
        }
    }
}