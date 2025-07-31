package kr.hhplus.be.server.order.infrastructure.persistence.adapter;

import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Balance 차감 영속성 Adapter (Order 도메인용)
 * Order 도메인에서 잔액 차감을 위한 어댑터
 * 새로운 balance 테이블 사용
 */
@Component("orderBalancePersistenceAdapter")
public class BalancePersistenceAdapter implements DeductBalancePort {

    private final BalanceJpaRepository balanceJpaRepository;

    public BalancePersistenceAdapter(BalanceJpaRepository balanceJpaRepository) {
        this.balanceJpaRepository = balanceJpaRepository;
    }

    @Override
    @Transactional
    public boolean deductBalance(Long userId, BigDecimal amount) {
        try {
            // 새로운 balance 테이블에서 잔액 조회
            BalanceEntity balance = balanceJpaRepository.findByUserIdAndStatus(userId, "ACTIVE")
                    .orElse(null);
            
            if (balance == null) {
                return false; // 잔액 정보가 없음
            }
            
            // 잔액 차감 (Optimistic Locking 자동 적용)
            boolean success = balance.deductAmount(amount);
            if (success) {
                balanceJpaRepository.save(balance);
            }
            return success;
            
        } catch (Exception e) {
            // OptimisticLockingFailureException 등 예외 처리
            return false;   
        }
    }
}