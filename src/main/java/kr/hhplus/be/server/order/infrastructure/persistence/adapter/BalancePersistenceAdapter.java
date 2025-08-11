package kr.hhplus.be.server.order.infrastructure.persistence.adapter;

import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.balance.infrastructure.persistence.entity.BalanceEntity;
import kr.hhplus.be.server.balance.infrastructure.persistence.repository.BalanceJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

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
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // 새로운 balance 테이블에서 잔액 조회 (낙관적 락 사용)
                BalanceEntity balance = balanceJpaRepository.findByUserIdAndStatus(userId, "ACTIVE")
                        .orElse(null);
                
                if (balance == null) {
                    System.out.println("DEBUG: 잔액 정보가 없습니다. userId: " + userId);
                    return false; // 잔액 정보가 없음
                }
                
                System.out.println("DEBUG: 차감 전 잔액: " + balance.getAmount() + ", 차감 금액: " + amount);
                
                // 잔액 차감 (Optimistic Locking 자동 적용)
                boolean success = balance.deductAmount(amount);
                if (success) {
                    balanceJpaRepository.save(balance);
                    System.out.println("DEBUG: 차감 후 잔액: " + balance.getAmount());
                    return true;
                } else {
                    System.out.println("DEBUG: 잔액 차감 실패 - 잔액 부족");
                    return false;
                }
                
            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                System.out.println("DEBUG: 옵티미스틱 락 충돌 발생. 재시도 " + retryCount + "/" + maxRetries);
                
                if (retryCount >= maxRetries) {
                    System.out.println("DEBUG: 최대 재시도 횟수 초과");
                    return false;
                }
                
                try {
                    Thread.sleep(50 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                
            } catch (Exception e) {
                System.out.println("DEBUG: 잔액 차감 중 예외 발생: " + e.getMessage());
                return false;   
            }
        }
        
        return false;
    }

    @Override
    @Transactional
    public boolean deductBalanceWithPessimisticLock(Long userId, BigDecimal amount) {
        try {
            // 비관적 락으로 잔액 조회
            BalanceEntity balance = balanceJpaRepository.findByUserIdAndStatusWithLock(userId, "ACTIVE")
                    .orElse(null);
            
            if (balance == null) {
                System.out.println("DEBUG: 잔액 정보가 없습니다. userId: " + userId);
                return false; // 잔액 정보가 없음
            }
            
            System.out.println("DEBUG: 차감 전 잔액: " + balance.getAmount() + ", 차감 금액: " + amount);
            
            // 잔액 차감 (비관적 락 적용)
            boolean success = balance.deductAmount(amount);
            if (success) {
                balanceJpaRepository.save(balance);
                System.out.println("DEBUG: 차감 후 잔액: " + balance.getAmount());
                return true;
            } else {
                System.out.println("DEBUG: 잔액 차감 실패 - 잔액 부족");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("DEBUG: 잔액 차감 중 예외 발생: " + e.getMessage());
            return false;   
        }
    }
}