package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.shared.lock.DistributedLockManager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * 잔액 충전 Application 서비스
 * - 분산 락으로 중복 클릭 방지
 * - 사용자별 순차 처리로 동시성 제어
 */
@Slf4j
@Service
public class ChargeBalanceService implements ChargeBalanceUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadBalancePort loadBalancePort;
    private final SaveBalanceTransactionPort saveBalanceTransactionPort;
    private final DistributedLockManager distributedLockManager;

    public ChargeBalanceService(LoadUserPort loadUserPort, 
                               LoadBalancePort loadBalancePort,
                               SaveBalanceTransactionPort saveBalanceTransactionPort,
                               DistributedLockManager distributedLockManager) {
        this.loadUserPort = loadUserPort;
        this.loadBalancePort = loadBalancePort;
        this.saveBalanceTransactionPort = saveBalanceTransactionPort;
        this.distributedLockManager = distributedLockManager;
    }

    /**
     * 잔액 충전 - 분산 락으로 중복 클릭 방지
     * 사용자별로 락을 걸어 동시 충전 요청을 순차 처리
     */
    @Override
    public ChargeBalanceResult chargeBalance(ChargeBalanceCommand command) {
        String lockKey = "balance-charge:" + command.getUserId();
        
        log.info("잔액 충전 시작 - 사용자: {}, 금액: {}", command.getUserId(), command.getAmount());
        
        try {
            // 분산 락으로 중복 클릭 방지 (3초 대기, 10초 보유)
            return distributedLockManager.executeWithLock(
                lockKey,
                Duration.ofSeconds(10), // 락 보유 시간
                Duration.ofSeconds(3),  // 락 대기 시간
                () -> performChargeBalanceWithTransaction(command)
            );
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Failed to acquire lock")) {
                log.warn("분산 락 획득 실패 - 사용자: {}, 다른 요청이 처리 중", command.getUserId());
                return ChargeBalanceResult.failure("동일한 요청이 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }
            log.error("잔액 충전 실패 - 사용자: {}, 오류: {}", command.getUserId(), e.getMessage(), e);
            return ChargeBalanceResult.failure("잔액 충전 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 트랜잭션 범위에서 실행되는 실제 잔액 충전 로직
     * 분산 락이 걸린 상태에서 호출됨
     */
    @Transactional
    private ChargeBalanceResult performChargeBalanceWithTransaction(ChargeBalanceCommand command) {
        // 1. 입력값 검증
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("잘못된 충전 금액 - 사용자: {}, 금액: {}", command.getUserId(), command.getAmount());
            return ChargeBalanceResult.failure("충전 금액은 0보다 커야 합니다.");
        }
        
        // 최대 충전 금액 제한 (예: 100만원)
        BigDecimal maxChargeAmount = new BigDecimal("1000000");
        if (command.getAmount().compareTo(maxChargeAmount) > 0) {
            log.warn("최대 충전 금액 초과 - 사용자: {}, 요청금액: {}, 최대금액: {}", 
                    command.getUserId(), command.getAmount(), maxChargeAmount);
            return ChargeBalanceResult.failure("한 번에 충전할 수 있는 최대 금액은 1,000,000원입니다.");
        }
        
        // 2. 사용자 존재 확인
        if (!loadUserPort.existsByUserId(command.getUserId())) {
            log.warn("존재하지 않는 사용자 - 사용자ID: {}", command.getUserId());
            return ChargeBalanceResult.failure("사용자를 찾을 수 없습니다.");
        }

        // 3. 잔액 조회 또는 생성
        Balance balance = loadBalancePort.loadActiveBalanceByUserId(command.getUserId())
                .orElseGet(() -> {
                    log.info("새로운 잔액 생성 - 사용자: {}", command.getUserId());
                    return Balance.builder().userId(command.getUserId()).build();
                });

        BigDecimal beforeAmount = balance.getAmount();
        
        // 4. 잔액 충전 (도메인 로직)
        balance.charge(command.getAmount());
        
        log.debug("잔액 충전 처리 - 사용자: {}, 이전잔액: {}, 충전금액: {}, 충전후잔액: {}", 
                command.getUserId(), beforeAmount, command.getAmount(), balance.getAmount());
        
        // 5. 낙관적 락으로 저장 (version 필드를 통한 동시성 제어)
        Balance savedBalance = loadBalancePort.saveBalanceWithConcurrencyControl(balance);

        // 6. 거래 내역 생성
        BalanceTransaction transaction = BalanceTransaction.create(
                command.getUserId(), 
                command.getAmount(), 
                BalanceTransaction.TransactionType.CHARGE,
                "잔액 충전"
        );
        BalanceTransaction savedTransaction = saveBalanceTransactionPort.saveBalanceTransaction(transaction);

        log.info("잔액 충전 완료 - 사용자: {}, 최종잔액: {}, 거래ID: {}", 
                command.getUserId(), savedBalance.getAmount(), savedTransaction.getId());

        return ChargeBalanceResult.success(
                command.getUserId(),
                savedBalance.getAmount(),
                savedTransaction.getId()
        );
    }
} 