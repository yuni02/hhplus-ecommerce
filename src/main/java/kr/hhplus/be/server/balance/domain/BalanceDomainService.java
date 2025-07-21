package kr.hhplus.be.server.balance.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 순수한 도메인 로직만 포함하는 도메인 서비스
 * 프레임워크나 외부 의존성 없음
 */
public class BalanceDomainService {

    /**
     * 잔액 충전 도메인 로직
     */
    public static Balance chargeBalance(Balance balance, BigDecimal amount) {
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
     * 잔액 차감 도메인 로직
     */
    public static Balance deductBalance(Balance balance, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("차감 금액은 양수여야 합니다.");
        }

        balance.deduct(amount);
        return balance;
    }

    /**
     * 거래 기록 생성 도메인 로직
     */
    public static BalanceTransaction createTransaction(Long userId, BigDecimal amount, 
                                                     BalanceTransaction.TransactionType type, 
                                                     String description) {
        BalanceTransaction transaction = new BalanceTransaction(userId, amount, type, description);
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }

    /**
     * 거래 상태 변경 도메인 로직
     */
    public static BalanceTransaction completeTransaction(BalanceTransaction transaction) {
        transaction.setStatus(BalanceTransaction.TransactionStatus.COMPLETED);
        transaction.setUpdatedAt(LocalDateTime.now());
        return transaction;
    }

    /**
     * 거래 실패 처리 도메인 로직
     */
    public static BalanceTransaction failTransaction(BalanceTransaction transaction) {
        transaction.setStatus(BalanceTransaction.TransactionStatus.FAILED);
        transaction.setUpdatedAt(LocalDateTime.now());
        return transaction;
    }

    /**
     * 잔액 충분 여부 확인 도메인 로직
     */
    public static boolean hasSufficientBalance(Balance balance, BigDecimal requiredAmount) {
        return balance.hasSufficientBalance(requiredAmount);
    }
} 