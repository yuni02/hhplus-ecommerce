package kr.hhplus.be.server.balance.application.port.out;

import kr.hhplus.be.server.balance.domain.Balance;
import java.util.Optional;

/**
 * 잔액 조회 Outgoing Port
 */
public interface LoadBalancePort {
    
    /**
     * 사용자 ID로 활성 잔액 조회
     */
    Optional<Balance> loadActiveBalanceByUserId(Long userId);
    
    /**
     * 잔액 저장
     */
    Balance saveBalance(Balance balance);
} 