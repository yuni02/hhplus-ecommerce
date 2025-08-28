package kr.hhplus.be.server.order.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 잔액 차감 요청 이벤트
 */
@Getter
public class BalanceDeductionRequestedEvent extends ApplicationEvent {
    
    private final String requestId;
    private final Long userId;
    private final BigDecimal amount;
    
    public BalanceDeductionRequestedEvent(Object source, String requestId, Long userId, BigDecimal amount) {
        super(source);
        this.requestId = requestId;
        this.userId = userId;
        this.amount = amount;
    }
}