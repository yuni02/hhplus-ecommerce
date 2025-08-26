package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.order.domain.CouponUsageRequestedEvent;
import kr.hhplus.be.server.order.domain.CouponUsageCompletedEvent;
import kr.hhplus.be.server.order.domain.CouponRestorationRequestedEvent;
import kr.hhplus.be.server.order.domain.CouponRestorationCompletedEvent;
import kr.hhplus.be.server.shared.event.SynchronousEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 쿠폰 이벤트 핸들러
 * 주문 서비스에서 발행한 쿠폰 사용 이벤트를 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventHandler {
    
    private final UseCouponUseCase useCouponUseCase;
    private final SynchronousEventProcessor synchronousEventProcessor;
    
    /**
     * 쿠폰 사용 요청 이벤트 처리
     */
    @EventListener
    public void handleCouponUsageRequested(CouponUsageRequestedEvent event) {
        log.debug("쿠폰 사용 요청 이벤트 처리 - requestId: {}, userId: {}, userCouponId: {}", 
                 event.getRequestId(), event.getUserId(), event.getUserCouponId());
        
        CouponUsageCompletedEvent responseEvent;
        
        try {
            // 기존 쿠폰 사용 로직 호출
            UseCouponUseCase.UseCouponCommand command = new UseCouponUseCase.UseCouponCommand(
                event.getUserId(), event.getUserCouponId(), event.getOrderAmount()
            );
            
            UseCouponUseCase.UseCouponResult result = useCouponUseCase.useCouponWithPessimisticLock(command);
            
            if (result.isSuccess()) {
                responseEvent = CouponUsageCompletedEvent.success(
                    this, 
                    event.getRequestId(),
                    event.getUserId(),
                    event.getUserCouponId(),
                    result.getDiscountedAmount(),
                    result.getDiscountAmount()
                );
                
                log.debug("쿠폰 사용 성공 - requestId: {}, discountAmount: {}", 
                         event.getRequestId(), result.getDiscountAmount());
            } else {
                responseEvent = CouponUsageCompletedEvent.failure(
                    this,
                    event.getRequestId(),
                    event.getUserId(), 
                    event.getUserCouponId(),
                    result.getErrorMessage()
                );
                
                log.warn("쿠폰 사용 실패 - requestId: {}, error: {}", 
                        event.getRequestId(), result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("쿠폰 사용 처리 중 예외 발생 - requestId: {}", event.getRequestId(), e);
            
            responseEvent = CouponUsageCompletedEvent.failure(
                this,
                event.getRequestId(),
                event.getUserId(),
                event.getUserCouponId(),
                "쿠폰 처리 중 예외가 발생했습니다: " + e.getMessage()
            );
        }
        
        // 응답 이벤트를 동기 처리기에 전달
        synchronousEventProcessor.handleResponse(event.getRequestId(), responseEvent);
    }
    
    /**
     * 쿠폰 복원 요청 이벤트 처리
     * 보상 트랜잭션 처리
     */
    @EventListener
    public void handleCouponRestoration(CouponRestorationRequestedEvent event) {
        log.debug("쿠폰 복원 요청 이벤트 처리 - requestId: {}, userId: {}, userCouponId: {}, reason: {}", 
                 event.getRequestId(), event.getUserId(), event.getUserCouponId(), event.getReason());
        
        CouponRestorationCompletedEvent responseEvent;
        
        try {
            // 쿠폰 복원 (사용 취소)
            // TODO: UseCouponUseCase에 복원 메소드 추가 필요
            // 임시로 로그만 남기고 성공 처리
            
            log.info("쿠폰 복원 시뮬레이션 - userId: {}, userCouponId: {}", 
                     event.getUserId(), event.getUserCouponId());
            
            responseEvent = CouponRestorationCompletedEvent.success(
                this, event.getRequestId(), event.getUserId(), event.getUserCouponId()
            );
            
            log.info("쿠폰 복원 성공 (시뮬레이션) - requestId: {}, userId: {}, userCouponId: {}", 
                     event.getRequestId(), event.getUserId(), event.getUserCouponId());
            
        } catch (Exception e) {
            log.error("쿠폰 복원 처리 중 예외 발생 - requestId: {}", event.getRequestId(), e);
            
            responseEvent = CouponRestorationCompletedEvent.failure(
                this, event.getRequestId(), event.getUserId(), event.getUserCouponId(),
                "쿠폰 복원 처리 중 예외가 발생했습니다: " + e.getMessage()
            );
        }
        
        // 응답 이벤트를 동기 처리기에 전달
        synchronousEventProcessor.handleResponse(event.getRequestId(), responseEvent);
    }
}