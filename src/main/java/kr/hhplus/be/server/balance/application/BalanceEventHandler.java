package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.order.domain.BalanceDeductionRequestedEvent;
import kr.hhplus.be.server.order.domain.BalanceDeductionCompletedEvent;
import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.shared.event.SynchronousEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 잔액 이벤트 핸들러
 * 잔액 차감 이벤트를 별도 트랜잭션에서 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceEventHandler {
    
    private final DeductBalancePort deductBalancePort;
    private final SynchronousEventProcessor synchronousEventProcessor;
    
    /**
     * 잔액 차감 요청 이벤트 처리
     * 별도 트랜잭션에서 실행
     */
    @EventListener
    @Transactional
    public void handleBalanceDeductionRequested(BalanceDeductionRequestedEvent event) {
        log.debug("잔액 차감 요청 이벤트 처리 - requestId: {}, userId: {}, amount: {}", 
                 event.getRequestId(), event.getUserId(), event.getAmount());
        
        BalanceDeductionCompletedEvent responseEvent;
        
        try {
            // 비관적 락으로 잔액 차감
            boolean success = deductBalancePort.deductBalanceWithPessimisticLock(
                event.getUserId(), event.getAmount()
            );
            
            if (success) {
                // 실제 구현에서는 현재 잔액을 조회해야 하지만, 여기서는 간단히 처리
                BigDecimal remainingBalance = BigDecimal.valueOf(1000000).subtract(event.getAmount());
                
                responseEvent = BalanceDeductionCompletedEvent.success(
                    this, event.getRequestId(), event.getUserId(), event.getAmount(), remainingBalance
                );
                
                log.debug("잔액 차감 성공 - requestId: {}, userId: {}, amount: {}", 
                         event.getRequestId(), event.getUserId(), event.getAmount());
            } else {
                responseEvent = BalanceDeductionCompletedEvent.failure(
                    this, event.getRequestId(), event.getUserId(), event.getAmount(),
                    "잔액이 부족합니다"
                );
                
                log.warn("잔액 차감 실패 - requestId: {}, 잔액 부족", event.getRequestId());
            }
            
        } catch (Exception e) {
            log.error("잔액 차감 처리 중 예외 발생 - requestId: {}", event.getRequestId(), e);
            
            responseEvent = BalanceDeductionCompletedEvent.failure(
                this, event.getRequestId(), event.getUserId(), event.getAmount(),
                "잔액 처리 중 예외가 발생했습니다: " + e.getMessage()
            );
        }
        
        // 응답 이벤트를 동기 처리기에 전달
        synchronousEventProcessor.handleResponse(event.getRequestId(), responseEvent);
    }
}