package kr.hhplus.be.server.order.application.port.out;

import java.math.BigDecimal;

/**
 * 잔액 차감 Outgoing Port
 */
public interface DeductBalancePort {
    
    /**
     * 잔액 차감 (동시성 제어 없음)
     */
    boolean deductBalance(Long userId, BigDecimal amount);
    
    /**
     * 잔액 차감 (비관적 락 적용)
     */
    boolean deductBalanceWithPessimisticLock(Long userId, BigDecimal amount);
} 