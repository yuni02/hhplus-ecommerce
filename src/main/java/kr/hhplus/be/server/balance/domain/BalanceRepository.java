package kr.hhplus.be.server.balance.domain;

import java.util.Optional;

public interface BalanceRepository {
    
    Optional<Balance> findByUserIdAndStatus(Long userId, Balance.BalanceStatus status);
    
    Optional<Balance> findActiveBalanceByUserId(Long userId);
    
    Balance save(Balance balance);
    
    Optional<Balance> findById(Long id);
} 