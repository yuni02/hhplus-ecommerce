package kr.hhplus.be.server.balance.domain;

import kr.hhplus.be.server.shared.domain.BaseEntity;

import java.math.BigDecimal;

public class Balance extends BaseEntity {

    private Long userId;
    private BigDecimal amount = BigDecimal.ZERO;
    private BalanceStatus status = BalanceStatus.ACTIVE;

    public Balance() {}

    public Balance(Long userId) {
        this.userId = userId;
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
        this.amount = this.amount.add(amount);
    }

    public void deduct(BigDecimal amount) {
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