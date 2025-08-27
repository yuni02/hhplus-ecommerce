package kr.hhplus.be.server.order.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class CouponUsageRequestedEvent extends ApplicationEvent {
    
    private final String requestId;
    private final Long userId;
    private final Long userCouponId;
    private final BigDecimal orderAmount;
    
    public CouponUsageRequestedEvent(Object source, String requestId, Long userId, 
                                   Long userCouponId, BigDecimal orderAmount) {
        super(source);
        this.requestId = requestId;
        this.userId = userId;
        this.userCouponId = userCouponId;
        this.orderAmount = orderAmount;
    }
}