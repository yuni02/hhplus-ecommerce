package kr.hhplus.be.server.balance.domain;

import java.math.BigDecimal;

/**
 * 잔액 충전 결과
 */
public class BalanceChargeResult {
    private final boolean success;
    private final Long userId;
    private final BigDecimal newBalance;
    private final Long transactionId;
    private final String errorMessage;
    
    public BalanceChargeResult(boolean success, Long userId, BigDecimal newBalance, Long transactionId, String errorMessage) {
        this.success = success;
        this.userId = userId;
        this.newBalance = newBalance;
        this.transactionId = transactionId;
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public BigDecimal getNewBalance() {
        return newBalance;
    }
    
    public Long getTransactionId() {
        return transactionId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public static BalanceChargeResult success(Long userId, BigDecimal newBalance, Long transactionId) {
        return new BalanceChargeResult(true, userId, newBalance, transactionId, null);
    }
    
    public static BalanceChargeResult failure(String errorMessage) {
        return new BalanceChargeResult(false, null, null, null, errorMessage);
    }
} 