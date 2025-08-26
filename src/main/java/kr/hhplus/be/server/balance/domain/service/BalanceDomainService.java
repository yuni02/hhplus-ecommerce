package kr.hhplus.be.server.balance.domain.service;

import kr.hhplus.be.server.order.domain.BalanceDeductionRequestedEvent;
import kr.hhplus.be.server.order.domain.BalanceDeductionCompletedEvent;
import kr.hhplus.be.server.shared.event.SynchronousEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 잔액 도메인 서비스
 * 잔액 관련 비즈니스 로직을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceDomainService {
    
    private final SynchronousEventProcessor synchronousEventProcessor;
    
    /**
     * 잔액 처리 (이벤트 기반 분산 트랜잭션)
     */
    public BalanceProcessResult processBalanceDeduction(Long userId, BigDecimal amount) {
        log.debug("잔액 처리 시작 - userId: {}, amount: {}", userId, amount);
        
        String requestId = "balance_" + userId + "_" + System.currentTimeMillis();
        BalanceDeductionRequestedEvent requestEvent = new BalanceDeductionRequestedEvent(
            this, requestId, userId, amount
        );
        
        try {
            BalanceDeductionCompletedEvent responseEvent = synchronousEventProcessor.publishAndWaitForResponse(
                requestEvent, requestId, BalanceDeductionCompletedEvent.class, 5
            );
            
            if (!responseEvent.isSuccess()) {
                log.warn("잔액 차감 실패 - userId: {}, amount: {}, error: {}", 
                        userId, amount, responseEvent.getErrorMessage());
                return BalanceProcessResult.failure(responseEvent.getErrorMessage());
            }
            
            log.debug("잔액 처리 완료 - userId: {}, remainingBalance: {}", 
                     userId, responseEvent.getRemainingBalance());
            return BalanceProcessResult.success(responseEvent.getRemainingBalance());
            
        } catch (Exception e) {
            log.error("잔액 이벤트 처리 실패 - userId: {}", userId, e);
            return BalanceProcessResult.failure("잔액 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    // 결과 클래스
    public static class BalanceProcessResult {
        private final boolean success;
        private final BigDecimal remainingBalance;
        private final String errorMessage;
        
        private BalanceProcessResult(boolean success, BigDecimal remainingBalance, String errorMessage) {
            this.success = success;
            this.remainingBalance = remainingBalance;
            this.errorMessage = errorMessage;
        }
        
        public static BalanceProcessResult success(BigDecimal remainingBalance) {
            return new BalanceProcessResult(true, remainingBalance, null);
        }
        
        public static BalanceProcessResult failure(String errorMessage) {
            return new BalanceProcessResult(false, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public BigDecimal getRemainingBalance() { return remainingBalance; }
        public String getErrorMessage() { return errorMessage; }
    }
}