package kr.hhplus.be.server.balance.domain;

import java.math.BigDecimal;

/**
 * 잔액 서비스 인터페이스
 * 잔액 관련 비즈니스 로직을 캡슐화
 */
public interface BalanceService {
    
    /**
     * 잔액 충전
     */
    BalanceChargeResult chargeBalance(Long userId, BigDecimal amount);
    
    /**
     * 잔액 차감
     */
    BalanceDeductResult deductBalance(Long userId, BigDecimal amount);
    
    /**
     * 잔액 조회
     */
    BalanceQueryResult getBalance(Long userId);
}

 