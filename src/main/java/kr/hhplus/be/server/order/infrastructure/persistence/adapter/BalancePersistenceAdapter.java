package kr.hhplus.be.server.order.infrastructure.persistence.adapter;

import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import kr.hhplus.be.server.user.infrastructure.persistence.repository.UserJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Balance 차감 영속성 Adapter (Order 도메인용)
 * Order 도메인에서 잔액 차감을 위한 어댑터
 */
@Component("orderBalancePersistenceAdapter")
public class BalancePersistenceAdapter implements DeductBalancePort {

    private final UserJpaRepository userJpaRepository;

    public BalancePersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional
    public boolean deductBalance(Long userId, BigDecimal amount) {
        try {
            UserEntity user = userJpaRepository.findByUserIdAndStatus(userId, "ACTIVE")
                    .orElse(null);
            
            if (user == null) {
                return false; // 잔액 정보가 없음
            }
            
            boolean success = user.deductAmount(amount);
            if (success) {
                userJpaRepository.save(user);
            }
            return success;
            
        } catch (Exception e) {
            return false;   
        }
    }
}