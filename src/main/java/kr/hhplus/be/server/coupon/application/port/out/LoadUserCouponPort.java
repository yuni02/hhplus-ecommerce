package kr.hhplus.be.server.coupon.application.port.out;

import java.util.List;

/**
 * 사용자 쿠폰 조회 Outgoing Port
 */
public interface LoadUserCouponPort {
    
    /**
     * 사용자 ID로 쿠폰 목록 조회
     */
    List<UserCouponInfo> loadUserCouponsByUserId(Long userId);
    
    /**
     * 사용자 쿠폰 ID로 개별 쿠폰 조회
     */
    java.util.Optional<kr.hhplus.be.server.coupon.domain.UserCoupon> loadUserCoupon(Long userCouponId);
    
    /**
     * 사용자 쿠폰 정보
     */
    class UserCouponInfo {
        private final Long id;
        private final Long userId;
        private final Long couponId;
        private final String status;
        private final String issuedAt;
        private final String usedAt;
        private final Long orderId;
        
        public UserCouponInfo(Long id, Long userId, Long couponId, String status,
                            String issuedAt, String usedAt, Long orderId) {
            this.id = id;
            this.userId = userId;
            this.couponId = couponId;
            this.status = status;
            this.issuedAt = issuedAt;
            this.usedAt = usedAt;
            this.orderId = orderId;
        }
        
        public Long getId() {
            return id;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public Long getCouponId() {
            return couponId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getIssuedAt() {
            return issuedAt;
        }
        
        public String getUsedAt() {
            return usedAt;
        }
        
        public Long getOrderId() {
            return orderId;
        }
    }
} 