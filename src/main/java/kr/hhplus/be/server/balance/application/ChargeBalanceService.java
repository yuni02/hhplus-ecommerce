package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 잔액 충전 Application 서비스
 */
@Service
public class ChargeBalanceService implements ChargeBalanceUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadBalancePort loadBalancePort;
    private final SaveBalanceTransactionPort saveBalanceTransactionPort;
    
    // 재시도 설정
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long INITIAL_DELAY_MS = 50;
    private static final double BACKOFF_MULTIPLIER = 1.5;

    public ChargeBalanceService(LoadUserPort loadUserPort, 
                               LoadBalancePort loadBalancePort,
                               SaveBalanceTransactionPort saveBalanceTransactionPort) {
        this.loadUserPort = loadUserPort;
        this.loadBalancePort = loadBalancePort;
        this.saveBalanceTransactionPort = saveBalanceTransactionPort;
    }

    @Override
    @Transactional
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 50, multiplier = 1.5)
    )
    public ChargeBalanceResult chargeBalance(ChargeBalanceCommand command) {
        return chargeBalanceWithOptimisticLock(command, 0);
    }
    
    /**
     * 낙관적 락 전략을 사용한 잔액 충전
     * 데드락을 방지하기 위해 비관적 락 대신 낙관적 락만 사용
     */
    private ChargeBalanceResult chargeBalanceWithOptimisticLock(ChargeBalanceCommand command, int attempt) {
        try {
            return performChargeBalanceWithOptimisticLock(command);
        } catch (OptimisticLockingFailureException e) {
            // 낙관적 락 실패 시 재시도
            if (attempt < MAX_RETRY_ATTEMPTS) {
                System.out.println("낙관적 락 충돌 발생. 재시도 " + (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS);
                
                // 지수 백오프 적용 (더 짧은 지연)
                long delay = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempt));
                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return ChargeBalanceResult.failure("재시도 중 인터럽트가 발생했습니다.");
                }
                
                // 재귀적으로 재시도
                return chargeBalanceWithOptimisticLock(command, attempt + 1);
            } else {
                System.out.println("최대 재시도 횟수 초과. 잔액 충전 실패");
                return ChargeBalanceResult.failure("동시성 충돌로 인해 잔액 충전에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }
        } catch (Exception e) {
            return ChargeBalanceResult.failure("잔액 충전 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 낙관적 락을 사용한 잔액 충전 로직
     * 데드락을 방지하기 위해 비관적 락 대신 낙관적 락만 사용
     */
    private ChargeBalanceResult performChargeBalanceWithOptimisticLock(ChargeBalanceCommand command) {
        // 1. 입력값 검증
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ChargeBalanceResult.failure("충전 금액은 0보다 커야 합니다.");
        }
        
        // 2. 사용자 존재 확인
        if (!loadUserPort.existsByUserId(command.getUserId())) {
            return ChargeBalanceResult.failure("사용자를 찾을 수 없습니다.");
        }

        // 3. 낙관적 락으로 잔액 조회 (데드락 방지)
        Balance balance = loadBalancePort.loadActiveBalanceByUserId(command.getUserId())
                .orElseGet(() -> Balance.builder().userId(command.getUserId()).build());

        // 4. 잔액 충전 (도메인 로직)
        balance.charge(command.getAmount());
        
        // 5. 낙관적 락으로 저장 (동시성 제어)
        // version 필드를 통해 동시 수정 감지
        Balance savedBalance = loadBalancePort.saveBalanceWithConcurrencyControl(balance);

        // 6. 거래 내역 생성
        BalanceTransaction transaction = BalanceTransaction.create(
                command.getUserId(), 
                command.getAmount(), 
                BalanceTransaction.TransactionType.CHARGE,
                "잔액 충전"
        );
        BalanceTransaction savedTransaction = saveBalanceTransactionPort.saveBalanceTransaction(transaction);

        return ChargeBalanceResult.success(
                command.getUserId(),
                savedBalance.getAmount(),
                savedTransaction.getId()
        );
    }
} 