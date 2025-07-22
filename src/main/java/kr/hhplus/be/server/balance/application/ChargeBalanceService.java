package kr.hhplus.be.server.balance.application;

import kr.hhplus.be.server.balance.application.port.in.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.port.out.LoadBalancePort;
import kr.hhplus.be.server.balance.application.port.out.LoadUserPort;
import kr.hhplus.be.server.balance.application.port.out.SaveBalanceTransactionPort;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceTransaction;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 잔액 충전 Application 서비스
 */
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

    @Override
    public ChargeBalanceResult chargeBalance(ChargeBalanceCommand command) {
        try {
            // 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return ChargeBalanceResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 잔액 조회 또는 생성
            Balance balance = loadBalancePort.loadActiveBalanceByUserId(command.getUserId())
                    .orElseGet(() -> new Balance(command.getUserId()));

            // 도메인 로직을 통한 잔액 충전
            balance = chargeBalance(balance, command.getAmount());

            // 거래 기록 생성
            BalanceTransaction transaction = createTransaction(
                    command.getUserId(), command.getAmount(), "잔액 충전");

            // 잔액 저장
            balance = loadBalancePort.saveBalance(balance);

            // 거래 완료 처리
            transaction = completeTransaction(transaction);
            transaction = saveBalanceTransactionPort.saveBalanceTransaction(transaction);

            return ChargeBalanceResult.success(balance.getUserId(), balance.getAmount(), transaction.getId());
        } catch (Exception e) {
            return ChargeBalanceResult.failure("잔액 충전 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 잔액 충전 도메인 로직
     */
    private Balance chargeBalance(Balance balance, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 양수여야 합니다.");
        }
        if (amount.compareTo(BigDecimal.valueOf(1000000)) > 0) {
            throw new IllegalArgumentException("1회 최대 충전 금액은 1,000,000원입니다.");
        }

        balance.charge(amount);
        return balance;
    }

    /**
     * 거래 기록 생성 도메인 로직
     */
    private BalanceTransaction createTransaction(Long userId, BigDecimal amount, String description) {
        BalanceTransaction transaction = new BalanceTransaction(userId, amount, 
                BalanceTransaction.TransactionType.CHARGE, description);
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
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