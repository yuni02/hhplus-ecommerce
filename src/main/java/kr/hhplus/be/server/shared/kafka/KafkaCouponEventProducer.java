package kr.hhplus.be.server.shared.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaCouponEventProducer {
    
    private static final String COUPON_ISSUE_TOPIC = "coupon-issue-events";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * 쿠폰 발급 이벤트 발행 (비동기)
     * 빠르게 이벤트만 발행하고 실제 처리는 Consumer에서 수행
     */
    public void publishCouponIssueEvent(CouponIssueMessage message) {
        String key = "coupon-" + message.getCouponId() + "-user-" + message.getUserId();
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(COUPON_ISSUE_TOPIC, key, message);
            
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("쿠폰 발급 이벤트 발행 실패 - couponId: {}, userId: {}", 
                    message.getCouponId(), message.getUserId(), ex);
            } else {
                log.debug("쿠폰 발급 이벤트 발행 성공 - couponId: {}, userId: {}, offset: {}", 
                    message.getCouponId(), message.getUserId(), 
                    result.getRecordMetadata().offset());
            }
        });
    }
}