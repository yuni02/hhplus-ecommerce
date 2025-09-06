package kr.hhplus.be.server.coupon.application.port.in;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 Incoming Port (Use Case)
 */
public interface IssueCouponUseCase {
    
    /**
     * 쿠폰 발급 실행
     */
    IssueCouponResult issueCoupon(IssueCouponCommand command);
    
    /**
     * 쿠폰 발급 명령
     */
    class IssueCouponCommand {
        private final Long userId;
        private final Long couponId;
        
        public IssueCouponCommand(Long userId, Long couponId) {
            this.userId = userId;
            this.couponId = couponId;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public Long getCouponId() {
            return couponId;
        }
    }
    
    /**
     * 쿠폰 발급 결과
     */
    class IssueCouponResult {
        private final boolean success;
        private final Long id;
        private final Long couponId;
        private final String couponName;
        private final Integer discountAmount;
        private final String status;
        private final LocalDateTime issuedAt;
        private final String errorMessage;
        
        private IssueCouponResult(boolean success, Long userCouponId, Long couponId, String couponName,
                                Integer discountAmount, String status, LocalDateTime issuedAt, String errorMessage) {
            this.success = success;
            this.id = userCouponId;
            this.couponId = couponId;
            this.couponName = couponName;
            this.discountAmount = discountAmount;
            this.status = status;
            this.issuedAt = issuedAt;
            this.errorMessage = errorMessage;
        }
        
        public static IssueCouponResult success(Long userCouponId, Long couponId, String couponName,
                                              Integer discountAmount, String status, LocalDateTime issuedAt) {
            return new IssueCouponResult(true, userCouponId, couponId, couponName, discountAmount, status, issuedAt, null);
        }
        
        public static IssueCouponResult failure(String errorMessage) {
            return new IssueCouponResult(false, null, null, null, null, null, null, errorMessage);
        }
        
        public static IssueCouponResult processing(Long couponId, String couponName, String message) {
            return new IssueCouponResult(true, null, couponId, couponName, null, "PROCESSING", null, message);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Long getId() {
            return id;
        }
        
        public Long getCouponId() {
            return couponId;
        }
        
        public String getCouponName() {
            return couponName;
        }
        
        public Integer getDiscountAmount() {
            return discountAmount;
        }
        
        public String getStatus() {
            return status;
        }
        
        public LocalDateTime getIssuedAt() {
            return issuedAt;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
} 