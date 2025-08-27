package kr.hhplus.be.server.order.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CouponRestorationCompletedEvent extends ApplicationEvent {
    
    private final String requestId;
    private final Long userId;
    private final Long userCouponId;
    private final boolean success;
    private final String errorMessage;
    
    public CouponRestorationCompletedEvent(Object source, String requestId, Long userId,
                                           Long userCouponId, boolean success, String errorMessage) {
        super(source);
        this.requestId = requestId;
        this.userId = userId;
        this.userCouponId = userCouponId;
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public static CouponRestorationCompletedEvent success(Object source, String requestId, 
                                                        Long userId, Long userCouponId) {
        return new CouponRestorationCompletedEvent(source, requestId, userId, userCouponId, true, null);
    }
    
    public static CouponRestorationCompletedEvent failure(Object source, String requestId, 
                                                        Long userId, Long userCouponId, String errorMessage) {
        return new CouponRestorationCompletedEvent(source, requestId, userId, userCouponId, false, errorMessage);
    }
}