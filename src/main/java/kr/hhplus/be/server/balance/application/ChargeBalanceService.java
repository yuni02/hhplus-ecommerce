package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;
import kr.hhplus.be.server.shared.lock.DistributedLock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

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

    public ChargeBalanceService(LoadUserPort loadUserPort, 
                               LoadBalancePort loadBalancePort,
                               SaveBalanceTransactionPort saveBalanceTransactionPort) {
        this.loadUserPort = loadUserPort;
        this.loadBalancePort = loadBalancePort;
        this.saveBalanceTransactionPort = saveBalanceTransactionPort;
    }

    /**
     * 잔액 충전 - 분산 락으로 사용자별 순차 처리
     * @DistributedLock 어노테이션으로 동시성 제어
     */
    @Override
    @DistributedLock(
        key = "balance-#{#command.userId}",
        waitTime = 1,              // 3초 → 1초 (빠른 실패)
        leaseTime = 5,             // 10초 → 5초 (락 홀드 시간 단축)
        timeUnit = TimeUnit.SECONDS,
        throwException = true
    )
    @Transactional
    public ChargeBalanceResult chargeBalance(ChargeBalanceCommand command) {
        log.info("잔액 충전 시작 - 사용자: {}, 금액: {}", command.getUserId(), command.getAmount());
        
        try {
            return performChargeBalanceWithTransaction(command);
        } catch (Exception e) {
            log.error("잔액 충전 실패 - 사용자: {}, 오류: {}", command.getUserId(), e.getMessage(), e);
            return ChargeBalanceResult.failure("잔액 충전 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 실제 잔액 충전 로직
     * AOP 분산 락과 트랜잭션이 적용된 상태에서 호출됨
     */
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
        
        // 5. 잔액 저장 (분산 락으로 동시성 제어됨)
        Balance savedBalance = loadBalancePort.saveBalance(balance);

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
                savedTransaction.getId(),
                command.getAmount()  // 충전 금액 추가
        );
    }
} 