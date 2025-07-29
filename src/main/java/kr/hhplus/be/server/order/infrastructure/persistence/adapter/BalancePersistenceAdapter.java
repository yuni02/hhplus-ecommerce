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
            BalanceEntity balance = balanceJpaRepository.findByUserId(userId)
                    .orElse(null);
            
            if (balance == null) {
                return false; // 잔액 정보가 없음
            }
            
            if (balance.getAmount().compareTo(amount) < 0) {
                return false; // 잔액 부족
            }
            
            balance.setAmount(balance.getAmount().subtract(amount));
            balanceJpaRepository.save(balance);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
}