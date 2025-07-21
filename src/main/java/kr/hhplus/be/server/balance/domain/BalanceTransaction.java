package kr.hhplus.be.server.balance.domain;

import kr.hhplus.be.server.shared.domain.BaseEntity;

import java.math.BigDecimal;

public class BalanceTransaction extends BaseEntity {

    private Long userId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status = TransactionStatus.PENDING;
    private String description;
    private Long referenceId; // 주문 ID, 쿠폰 ID 등 참조

    public BalanceTransaction() {}

    public BalanceTransaction(Long userId, BigDecimal amount, TransactionType type, String description) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.description = description;
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public enum TransactionType {
        CHARGE, DEDUCT, REFUND
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED
    }
} 