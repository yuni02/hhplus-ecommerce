package kr.hhplus.be.server.balance.domain;

import java.math.BigDecimal;

/**
 * 잔액 차감 결과
 */
public class BalanceDeductResult {
    private final boolean success;
    private final Long userId;
    private final BigDecimal remainingBalance;
    private final Long transactionId;
    private final String errorMessage;
    
    public BalanceDeductResult(boolean success, Long userId, BigDecimal remainingBalance, Long transactionId, String errorMessage) {
        this.success = success;
        this.userId = userId;
        this.remainingBalance = remainingBalance;
        this.transactionId = transactionId;
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }
    
    public Long getTransactionId() {
        return transactionId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public static BalanceDeductResult success(Long userId, BigDecimal remainingBalance, Long transactionId) {
        return new BalanceDeductResult(true, userId, remainingBalance, transactionId, null);
    }
    
    public static BalanceDeductResult failure(String errorMessage) {
        return new BalanceDeductResult(false, null, null, null, errorMessage);
    }
} 