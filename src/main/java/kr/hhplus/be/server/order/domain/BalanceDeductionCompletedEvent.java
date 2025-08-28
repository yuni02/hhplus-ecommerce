package kr.hhplus.be.server.order.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 잔액 차감 완료 이벤트
 */
@Getter
public class BalanceDeductionCompletedEvent extends ApplicationEvent {
    
    private final String requestId;
    private final Long userId;
    private final BigDecimal amount;
    private final boolean success;
    private final BigDecimal remainingBalance;
    private final String errorMessage;
    
    private BalanceDeductionCompletedEvent(Object source, String requestId, Long userId, BigDecimal amount,
                                          boolean success, BigDecimal remainingBalance, String errorMessage) {
        super(source);
        this.requestId = requestId;
        this.userId = userId;
        this.amount = amount;
        this.success = success;
        this.remainingBalance = remainingBalance;
        this.errorMessage = errorMessage;
    }
    
    public static BalanceDeductionCompletedEvent success(Object source, String requestId, Long userId, 
                                                        BigDecimal amount, BigDecimal remainingBalance) {
        return new BalanceDeductionCompletedEvent(source, requestId, userId, amount, true, remainingBalance, null);
    }
    
    public static BalanceDeductionCompletedEvent failure(Object source, String requestId, Long userId, 
                                                        BigDecimal amount, String errorMessage) {
        return new BalanceDeductionCompletedEvent(source, requestId, userId, amount, false, null, errorMessage);
    }
}