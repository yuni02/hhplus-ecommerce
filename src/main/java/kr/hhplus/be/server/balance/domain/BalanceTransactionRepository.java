package kr.hhplus.be.server.balance.domain;

import java.util.List;
import java.util.Optional;

public interface BalanceTransactionRepository {
    
    List<BalanceTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<BalanceTransaction> findCompletedTransactionsByUserId(Long userId);
    
    BalanceTransaction save(BalanceTransaction transaction);
    
    Optional<BalanceTransaction> findById(Long id);
} 