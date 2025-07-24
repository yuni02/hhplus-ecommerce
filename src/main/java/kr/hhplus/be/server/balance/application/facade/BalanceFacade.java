package kr.hhplus.be.server.balance.application.facade;

import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.in.GetBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 잔액 관리 Facade
 * 복잡한 잔액 관련 로직을 단순화하여 제공
 */
@Service
public class BalanceFacade {

    private final LoadUserPort loadUserPort;
    private final LoadBalancePort loadBalancePort;
    private final SaveBalanceTransactionPort saveBalanceTransactionPort;
    
    private final AtomicLong transactionIdGenerator = new AtomicLong(1);

    public BalanceFacade(LoadUserPort loadUserPort, 
                        LoadBalancePort loadBalancePort,
                        SaveBalanceTransactionPort saveBalanceTransactionPort) {
        this.loadUserPort = loadUserPort;
        this.loadBalancePort = loadBalancePort;
        this.saveBalanceTransactionPort = saveBalanceTransactionPort;
    }

    /**
     * 잔액 조회 (Facade 메서드)
     */
    public Optional<GetBalanceUseCase.GetBalanceResult> getBalance(GetBalanceUseCase.GetBalanceCommand command) {
        try {
            // 1. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return Optional.empty();
            }

            // 2. 잔액 조회
            Optional<Balance> balanceOpt = loadBalancePort.loadActiveBalanceByUserId(command.getUserId());
            
            if (balanceOpt.isEmpty()) {
                return Optional.empty();
            }

            Balance balance = balanceOpt.get();
            return Optional.of(new GetBalanceUseCase.GetBalanceResult(command.getUserId(), balance.getAmount()));

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 잔액 충전 (Facade 메서드)
     */
    @Transactional
    public ChargeBalanceUseCase.ChargeBalanceResult chargeBalance(ChargeBalanceUseCase.ChargeBalanceCommand command) {
        try {
            // 1. 입력값 검증
            if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ChargeBalanceUseCase.ChargeBalanceResult.failure("충전 금액은 0보다 커야 합니다.");
            }
            
            // 2. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return ChargeBalanceUseCase.ChargeBalanceResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 2. 기존 잔액 조회 또는 새로 생성
            Balance balance = loadBalancePort.loadActiveBalanceByUserId(command.getUserId())
                    .orElse(new Balance(command.getUserId()));

            // 3. 잔액 충전
            Balance chargedBalance = chargeBalance(balance, command.getAmount());
            Balance savedBalance = loadBalancePort.saveBalance(chargedBalance);

            // 4. 거래 내역 생성
            BalanceTransaction transaction = createTransaction(command.getUserId(), command.getAmount(), "잔액 충전");
            BalanceTransaction savedTransaction = saveBalanceTransactionPort.saveBalanceTransaction(transaction);

            return ChargeBalanceUseCase.ChargeBalanceResult.success(
                    command.getUserId(),
                    savedBalance.getAmount(),
                    savedTransaction.getId()
            );

        } catch (Exception e) {
            return ChargeBalanceUseCase.ChargeBalanceResult.failure("잔액 충전 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 잔액 충전 도메인 로직
     */
    private Balance chargeBalance(Balance balance, BigDecimal amount) {
        balance.charge(amount);
        return balance;
    }

    /**
     * 거래 내역 생성 도메인 로직
     */
    private BalanceTransaction createTransaction(Long userId, BigDecimal amount, String description) {
        BalanceTransaction transaction = new BalanceTransaction(
                userId, 
                amount, 
                BalanceTransaction.TransactionType.CHARGE, 
                description
        );
        transaction.setId(transactionIdGenerator.getAndIncrement());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        return completeTransaction(transaction);
    }

    /**
     * 거래 완료 처리 도메인 로직
     */
    private BalanceTransaction completeTransaction(BalanceTransaction transaction) {
        transaction.setStatus(BalanceTransaction.TransactionStatus.COMPLETED);
        transaction.setUpdatedAt(LocalDateTime.now());
        return transaction;
    }
} 