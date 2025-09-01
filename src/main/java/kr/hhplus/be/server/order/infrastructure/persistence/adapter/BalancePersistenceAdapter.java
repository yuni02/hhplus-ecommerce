package kr.hhplus.be.server.order.infrastructure.persistence.adapter;

import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;
import kr.hhplus.be.server.shared.lock.DistributedLock;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Balance 차감 영속성 Adapter (Order 도메인용)
 * Order 도메인에서 잔액 차감을 위한 어댑터
 * 새로운 balance 테이블 사용
 */
@Slf4j
@Component("orderBalancePersistenceAdapter")
public class BalancePersistenceAdapter implements DeductBalancePort {

    private final BalanceJpaRepository balanceJpaRepository;

    public BalancePersistenceAdapter(BalanceJpaRepository balanceJpaRepository) {
        this.balanceJpaRepository = balanceJpaRepository;
    }

    @Override
    @DistributedLock(
        key = "balance-#{#userId}",
        waitTime = 3,
        leaseTime = 10,
        timeUnit = TimeUnit.SECONDS,
        throwException = true
    )
    @Transactional
    public boolean deductBalance(Long userId, BigDecimal amount) {
        log.debug("잔액 차감 시작 - 사용자: {}, 금액: {}", userId, amount);
        
        try {
            log.debug("분산 락 획득 후 잔액 차감 실행 - 사용자: {}", userId);
            
            // 새로운 balance 테이블에서 잔액 조회 (낙관적 락 사용)
            BalanceEntity balance = balanceJpaRepository.findByUserIdAndStatus(userId, "ACTIVE")
                    .orElse(null);
            
            if (balance == null) {
                log.debug("잔액 정보가 없습니다. userId: {}", userId);
                return false; // 잔액 정보가 없음
            }
            
            log.debug("차감 전 잔액: {}, 차감 금액: {}", balance.getAmount(), amount);
            
            // 잔액 차감 (Optimistic Locking 자동 적용)
            boolean success = balance.deductAmount(amount);
            if (success) {
                balanceJpaRepository.save(balance);
                log.debug("차감 후 잔액: {}", balance.getAmount());
                return true;
            } else {
                log.debug("잔액 차감 실패 - 잔액 부족");
                return false;
            }
            
        } catch (Exception e) {
            log.warn("잔액 차감 중 예외 발생: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deductBalanceWithPessimisticLock(Long userId, BigDecimal amount) {
        try {
            // 비관적 락으로 잔액 조회
            BalanceEntity balance = balanceJpaRepository.findByUserIdAndStatusWithLock(userId, "ACTIVE")
                    .orElse(null);
            
            if (balance == null) {
                log.debug("잔액 정보가 없습니다. userId: {}", userId);
                return false; // 잔액 정보가 없음
            }
            
            log.debug("차감 전 잔액: {}, 차감 금액: {}", balance.getAmount(), amount);
            
            // 잔액 차감 (비관적 락 적용)
            boolean success = balance.deductAmount(amount);
            if (success) {
                balanceJpaRepository.save(balance);
                log.debug("차감 후 잔액: {}", balance.getAmount());
                return true;
            } else {
                log.debug("잔액 차감 실패 - 잔액 부족");
                return false;
            }
            
        } catch (Exception e) {
            log.warn("잔액 차감 중 예외 발생: {}", e.getMessage());
            return false;   
        }
    }
}