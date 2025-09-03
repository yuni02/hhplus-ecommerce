package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.shared.kafka.CouponIssueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Kafka 기반 쿠폰 발급 이벤트 핸들러
 * 실제 쿠폰 발급 처리를 비동기로 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaCouponIssueEventHandler {
    
    private final LoadUserPort loadUserPort;
    private final LoadCouponPort loadCouponPort;
    private final SaveUserCouponPort saveUserCouponPort;
    private final RedisCouponService redisCouponService;
    private final RedisCouponQueueService queueService;
    
    @KafkaListener(
        topics = "coupon-issue-events",
        groupId = "coupon-issue-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleCouponIssueEvent(
            @Payload CouponIssueMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.info("쿠폰 발급 이벤트 수신 - topic: {}, partition: {}, offset: {}, couponId: {}, userId: {}",
                topic, partition, offset, message.getCouponId(), message.getUserId());
        
        try {
            processCouponIssuance(message);
            acknowledgment.acknowledge();
            log.info("쿠폰 발급 처리 완료 - couponId: {}, userId: {}", 
                    message.getCouponId(), message.getUserId());
        } catch (Exception e) {
            log.error("쿠폰 발급 처리 실패 - couponId: {}, userId: {}", 
                    message.getCouponId(), message.getUserId(), e);
            // 실패 시 Redis에 실패 결과 저장
            queueService.saveIssueResult(
                message.getCouponId(), 
                message.getUserId(), 
                false, 
                "쿠폰 발급 처리 중 오류가 발생했습니다: " + e.getMessage()
            );
            // 재시도 또는 DLQ로 전송하는 로직 추가 가능
            throw e;
        }
    }
    
    private void processCouponIssuance(CouponIssueMessage message) {
        Long couponId = message.getCouponId();
        Long userId = message.getUserId();
        
        // 1. Redis 기반 선착순 체크 및 발급 수량 원자적 증가
        RedisCouponService.CouponIssueResult redisResult = 
            redisCouponService.checkAndIssueCoupon(couponId, userId, message.getMaxIssuanceCount());
        
        if (!redisResult.isSuccess()) {
            log.warn("Redis 선착순 체크 실패 - couponId: {}, userId: {}, message: {}", 
                    couponId, userId, redisResult.getErrorMessage());
            
            queueService.saveIssueResult(couponId, userId, false, redisResult.getErrorMessage());
            return;
        }
        
        // 2. DB에서 쿠폰 정보 확인 및 발급 수량 증가
        boolean dbUpdated = loadCouponPort.incrementIssuedCount(couponId);
        if (!dbUpdated) {
            log.error("DB 쿠폰 발급 수량 증가 실패 - couponId: {}, userId: {}", couponId, userId);
            
            // Redis 롤백
            redisCouponService.rollbackCouponIssuance(couponId, userId);
            
            queueService.saveIssueResult(couponId, userId, false, "쿠폰이 모두 소진되었습니다.");
            return;
        }
        
        // 3. 사용자 쿠폰 생성 및 저장
        try {
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(userId)
                    .couponId(couponId)
                    .discountAmount(message.getDiscountAmount())
                    .issuedAt(LocalDateTime.now())
                    .build();
            
            UserCoupon savedUserCoupon = saveUserCouponPort.saveUserCoupon(userCoupon);
            
            // 4. Redis 캐시 업데이트
            redisCouponService.updateCouponIssuedCount(couponId, null);
            
            // 5. 발급 성공 결과 저장
            queueService.saveIssueResult(
                couponId, 
                userId, 
                true, 
                String.format("쿠폰 발급 완료 - 쿠폰ID: %d", savedUserCoupon.getId())
            );
            
            log.info("쿠폰 발급 성공 - userCouponId: {}, couponId: {}, userId: {}", 
                    savedUserCoupon.getId(), couponId, userId);
                    
        } catch (Exception e) {
            // 사용자 쿠폰 저장 실패 시 롤백
            log.error("사용자 쿠폰 저장 실패, 롤백 수행 - couponId: {}, userId: {}", couponId, userId, e);
            
            // DB 롤백
            loadCouponPort.decrementIssuedCount(couponId);
            
            // Redis 롤백
            redisCouponService.rollbackCouponIssuance(couponId, userId);
            
            throw e;
        }
    }
}