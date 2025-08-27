package kr.hhplus.be.server.order.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class CouponUsageCompletedEvent extends ApplicationEvent {
    
    private final String requestId;
    private final Long userId;
    private final Long userCouponId;
    private final boolean success;
    private final BigDecimal discountedAmount;
    private final Integer discountAmount;
    private final String errorMessage;
    
    public CouponUsageCompletedEvent(Object source, String requestId, Long userId,
                                     Long userCouponId, boolean success,
                                     BigDecimal discountedAmount, Integer discountAmount,
                                     String errorMessage) {
        super(source);
        this.requestId = requestId;
        this.userId = userId;
        this.userCouponId = userCouponId;
        this.success = success;
        this.discountedAmount = discountedAmount;
        this.discountAmount = discountAmount;
        this.errorMessage = errorMessage;
    }
    
    public static CouponUsageCompletedEvent success(Object source, String requestId, 
                                                  Long userId, Long userCouponId, 
                                                  BigDecimal discountedAmount, Integer discountAmount) {
        return new CouponUsageCompletedEvent(source, requestId, userId, userCouponId, 
                                           true, discountedAmount, discountAmount, null);
    }
    
    public static CouponUsageCompletedEvent failure(Object source, String requestId, 
                                                  Long userId, Long userCouponId, String errorMessage) {
        return new CouponUsageCompletedEvent(source, requestId, userId, userCouponId, 
                                           false, null, null, errorMessage);
    }
}