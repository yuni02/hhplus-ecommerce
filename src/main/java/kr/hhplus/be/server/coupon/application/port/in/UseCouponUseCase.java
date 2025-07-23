package kr.hhplus.be.server.coupon.application.port.in;

import java.math.BigDecimal;

/**
 * 쿠폰 사용 Incoming Port (Use Case)
 */
public interface UseCouponUseCase {
    
    /**
     * 쿠폰 사용 실행
     */
    UseCouponResult useCoupon(UseCouponCommand command);
    
    /**
     * 쿠폰 사용 명령
     */
    class UseCouponCommand {
        private final Long userId;
        private final Long userCouponId;
        private final BigDecimal orderAmount;
        
        public UseCouponCommand(Long userId, Long userCouponId, BigDecimal orderAmount) {
            this.userId = userId;
            this.userCouponId = userCouponId;
            this.orderAmount = orderAmount;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public Long getUserCouponId() {
            return userCouponId;
        }
        
        public BigDecimal getOrderAmount() {
            return orderAmount;
        }
    }
    
    /**
     * 쿠폰 사용 결과
     */
    class UseCouponResult {
        private final boolean success;
        private final BigDecimal discountedAmount;
        private final Integer discountAmount;
        private final String errorMessage;
        
        private UseCouponResult(boolean success, BigDecimal discountedAmount, 
                              Integer discountAmount, String errorMessage) {
            this.success = success;
            this.discountedAmount = discountedAmount;
            this.discountAmount = discountAmount;
            this.errorMessage = errorMessage;
        }
        
        public static UseCouponResult success(BigDecimal discountedAmount, Integer discountAmount) {
            return new UseCouponResult(true, discountedAmount, discountAmount, null);
        }
        
        public static UseCouponResult failure(String errorMessage) {
            return new UseCouponResult(false, null, null, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public BigDecimal getDiscountedAmount() {
            return discountedAmount;
        }
        
        public Integer getDiscountAmount() {
            return discountAmount;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
} 