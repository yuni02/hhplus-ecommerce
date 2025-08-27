package kr.hhplus.be.server.order.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CouponRestorationRequestedEvent extends ApplicationEvent {
    
    private final String requestId;
    private final Long userId;
    private final Long userCouponId;
    private final String reason;
    
    public CouponRestorationRequestedEvent(Object source, String requestId, Long userId, 
                                         Long userCouponId, String reason) {
        super(source);
        this.requestId = requestId;
        this.userId = userId;
        this.userCouponId = userCouponId;
        this.reason = reason;
    }
}