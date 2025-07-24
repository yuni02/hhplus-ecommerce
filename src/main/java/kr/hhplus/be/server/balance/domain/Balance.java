package kr.hhplus.be.server.balance.domain;

import java.math.BigDecimal;

/**
 * 잔액 도메인 엔티티
 * 순수한 비즈니스 로직만 포함
 */
public class Balance {

    private Long id;
    private Long userId;
    private BigDecimal amount = BigDecimal.ZERO;
    private BalanceStatus status = BalanceStatus.ACTIVE;

    public Balance() {}

    public Balance(Long userId) {
        this.userId = userId;
    }

    public Balance(Long id, Long userId, BigDecimal amount, BalanceStatus status) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BalanceStatus getStatus() {
        return status;
    }

    public void setStatus(BalanceStatus status) {
        this.status = status;
    }

    public void charge(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.amount = this.amount.add(amount);
    }

    public void deduct(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }
        if (this.amount.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("잔액이 부족합니다.");
        }
        this.amount = this.amount.subtract(amount);
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.amount.compareTo(amount) >= 0;
    }

    public enum BalanceStatus {
        ACTIVE, INACTIVE
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }
} 