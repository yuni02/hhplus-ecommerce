package kr.hhplus.be.server.coupon.infrastructure.scheduler;

import kr.hhplus.be.server.coupon.domain.service.RedisCouponQueueService;
import kr.hhplus.be.server.shared.kafka.CouponIssueMessage;
import kr.hhplus.be.server.shared.kafka.KafkaCouponEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis 쿠폰 대기열 처리 스케줄러
 * 주기적으로 Redis 큐에서 쿠폰 발급 요청을 꺼내서 Kafka로 발행
 */
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
    @Scheduled(fixedDelay = 1000) // 1초마다 실행
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
            // 큐 키에서 쿠폰 ID 추출: "coupon:queue:123" -> "123"
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
                        .maxIssuanceCount(100) // 기본값, 실제로는 쿠폰 정보에서 가져와야 함
                        .discountAmount(1000)   // 기본값, 실제로는 쿠폰 정보에서 가져와야 함
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