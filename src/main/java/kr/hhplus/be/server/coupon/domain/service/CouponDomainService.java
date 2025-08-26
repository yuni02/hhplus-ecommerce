package kr.hhplus.be.server.coupon.domain.service;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.domain.CouponUsageRequestedEvent;
import kr.hhplus.be.server.order.domain.CouponUsageCompletedEvent;
import kr.hhplus.be.server.order.domain.CouponRestorationRequestedEvent;
import kr.hhplus.be.server.order.domain.CouponRestorationCompletedEvent;
import kr.hhplus.be.server.shared.event.SynchronousEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 쿠폰 도메인 서비스
 * 쿠폰 관련 비즈니스 로직을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponDomainService {
    
    private final SynchronousEventProcessor synchronousEventProcessor;
    
    /**
     * 쿠폰 할인 처리 (이벤트 기반)
     */
    public CouponProcessResult processCouponDiscount(CreateOrderUseCase.CreateOrderCommand command, 
                                                   BigDecimal totalAmount) {
        if (command.getUserCouponId() == null) {
            log.debug("쿠폰 미사용 - userId: {}", command.getUserId());
            return CouponProcessResult.success(totalAmount, 0);
        }
        
        log.debug("쿠폰 처리 시작 - userId: {}, userCouponId: {}, totalAmount: {}", 
                 command.getUserId(), command.getUserCouponId(), totalAmount);
        
        // 요청 ID 생성
        String requestId = "coupon_" + command.getUserId() + "_" + System.currentTimeMillis();
        
        // 쿠폰 사용 이벤트 발행
        CouponUsageRequestedEvent requestEvent = new CouponUsageRequestedEvent(
            this, requestId, command.getUserId(), command.getUserCouponId(), totalAmount
        );
        
        try {
            // 동기 이벤트 처리 (5초 타임아웃)
            CouponUsageCompletedEvent responseEvent = synchronousEventProcessor.publishAndWaitForResponse(
                requestEvent, requestId, CouponUsageCompletedEvent.class, 5
            );
            
            if (!responseEvent.isSuccess()) {
                log.warn("쿠폰 사용 실패 - userId: {}, userCouponId: {}, error: {}", 
                        command.getUserId(), command.getUserCouponId(), responseEvent.getErrorMessage());
                return CouponProcessResult.failure(responseEvent.getErrorMessage());
            }
            
            log.debug("쿠폰 처리 완료 - userId: {}, discountAmount: {}", 
                     command.getUserId(), responseEvent.getDiscountAmount());
            return CouponProcessResult.success(responseEvent.getDiscountedAmount(), responseEvent.getDiscountAmount());
            
        } catch (Exception e) {
            log.error("쿠폰 이벤트 처리 실패 - userId: {}, userCouponId: {}", 
                     command.getUserId(), command.getUserCouponId(), e);
            return CouponProcessResult.failure("쿠폰 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 쿠폰 복원 (보상 트랜잭션)
     */
    public void rollbackCouponUsage(CreateOrderUseCase.CreateOrderCommand command, String reason) {
        if (command.getUserCouponId() == null) {
            return; // 쿠폰을 사용하지 않은 경우
        }
        
        log.warn("쿠폰 복원 시작 - userId: {}, userCouponId: {}, 사유: {}", 
                 command.getUserId(), command.getUserCouponId(), reason);
        
        String requestId = "coupon_rollback_" + command.getUserCouponId() + "_" + System.currentTimeMillis();
        
        CouponRestorationRequestedEvent rollbackEvent = new CouponRestorationRequestedEvent(
            this, requestId, command.getUserId(), command.getUserCouponId(), reason
        );
        
        try {
            // 동기 이벤트 처리로 복원 진행
            CouponRestorationCompletedEvent responseEvent = synchronousEventProcessor.publishAndWaitForResponse(
                rollbackEvent, requestId, CouponRestorationCompletedEvent.class, 3
            );
            
            if (responseEvent.isSuccess()) {
                log.info("쿠폰 복원 성공 - userId: {}, userCouponId: {}", 
                         command.getUserId(), command.getUserCouponId());
            } else {
                log.error("쿠폰 복원 실패 - userId: {}, userCouponId: {}, error: {}", 
                          command.getUserId(), command.getUserCouponId(), responseEvent.getErrorMessage());
                // TODO: 쿠폰 복원 실패 시 알림 또는 매뉴얼 처리 필요
            }
            
        } catch (Exception e) {
            log.error("쿠폰 복원 이벤트 처리 실패 - userId: {}, userCouponId: {}", 
                     command.getUserId(), command.getUserCouponId(), e);
        }
    }
    
    // 결과 클래스
    public static class CouponProcessResult {
        private final boolean success;
        private final BigDecimal discountedAmount;
        private final Integer discountAmount;
        private final String errorMessage;
        
        private CouponProcessResult(boolean success, BigDecimal discountedAmount, 
                                  Integer discountAmount, String errorMessage) {
            this.success = success;
            this.discountedAmount = discountedAmount;
            this.discountAmount = discountAmount;
            this.errorMessage = errorMessage;
        }
        
        public static CouponProcessResult success(BigDecimal discountedAmount, Integer discountAmount) {
            return new CouponProcessResult(true, discountedAmount, discountAmount, null);
        }
        
        public static CouponProcessResult failure(String errorMessage) {
            return new CouponProcessResult(false, null, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public BigDecimal getDiscountedAmount() { return discountedAmount; }
        public Integer getDiscountAmount() { return discountAmount; }
        public String getErrorMessage() { return errorMessage; }
    }
}