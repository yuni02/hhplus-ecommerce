package kr.hhplus.be.server.balance.domain;

import java.math.BigDecimal;

/**
 * 잔액 조회 결과
 */
public class BalanceQueryResult {
    private final boolean found;
    private final Long userId;
    private final BigDecimal balance;
    private final String errorMessage;
    
    public BalanceQueryResult(boolean found, Long userId, BigDecimal balance, String errorMessage) {
        this.found = found;
        this.userId = userId;
        this.balance = balance;
        this.errorMessage = errorMessage;
    }
    
    public boolean isFound() {
        return found;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public static BalanceQueryResult found(Long userId, BigDecimal balance) {
        return new BalanceQueryResult(true, userId, balance, null);
    }
    
    public static BalanceQueryResult notFound(String errorMessage) {
        return new BalanceQueryResult(false, null, null, errorMessage);
    }
} 