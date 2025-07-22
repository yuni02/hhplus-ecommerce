package kr.hhplus.be.server.order.application.port.out;

import java.math.BigDecimal;

/**
 * 잔액 차감 Outgoing Port
 */
public interface DeductBalancePort {
    
    /**
     * 잔액 차감
     */
    boolean deductBalance(Long userId, BigDecimal amount);
} 