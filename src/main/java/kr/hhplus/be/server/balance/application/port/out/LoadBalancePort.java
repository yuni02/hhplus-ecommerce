package kr.hhplus.be.server.balance.application.port.out;

import kr.hhplus.be.server.balance.domain.Balance;

import java.util.Optional;

/**
 * 잔액 조회 Port
 */
public interface LoadBalancePort {
    
    /**
     * 사용자 ID로 활성 잔액 조회
     */
    Optional<Balance> loadActiveBalanceByUserId(Long userId);
    
    /**
     * 사용자 ID로 활성 잔액 조회 (동시성 제어용)
     */
    Optional<Balance> loadActiveBalanceByUserIdWithLock(Long userId);
    
    /**
     * 잔액 저장
     */
    Balance saveBalance(Balance balance);
    
    /**
     * 잔액 저장 (동시성 제어용)
     */
    Balance saveBalanceWithConcurrencyControl(Balance balance);
} 