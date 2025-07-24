package kr.hhplus.be.server.coupon.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 쿠폰 조회 Incoming Port (Use Case)
 */
public interface GetUserCouponsUseCase {
    
    /**
     * 사용자 쿠폰 조회 실행
     */
    GetUserCouponsResult getUserCoupons(GetUserCouponsCommand command);
    
    /**
     * 사용자 쿠폰 조회 명령
     */
    class GetUserCouponsCommand {
        private final Long userId;
        
        public GetUserCouponsCommand(Long userId) {
            this.userId = userId;
        }
        
        public Long getUserId() {
            return userId;
        }
    }
    
    /**
     * 사용자 쿠폰 조회 결과
     */
    class GetUserCouponsResult {
        private final List<UserCouponInfo> userCoupons;
        
        public GetUserCouponsResult(List<UserCouponInfo> userCoupons) {
            this.userCoupons = userCoupons;
        }
        
        public List<UserCouponInfo> getUserCoupons() {
            return userCoupons;
        }
    }
    
    /**
     * 사용자 쿠폰 정보
     */
    class UserCouponInfo {
        private final Long userCouponId;
        private final Long couponId;
        private final String couponName;
        private final Integer discountAmount;
        private final String status;
        private final LocalDateTime issuedAt;
        private final LocalDateTime usedAt;
        
        public UserCouponInfo(Long userCouponId, Long couponId, String couponName,
                             Integer discountAmount, String status,
                             LocalDateTime issuedAt, LocalDateTime usedAt) {
            this.userCouponId = userCouponId;
            this.couponId = couponId;
            this.couponName = couponName;
            this.discountAmount = discountAmount;
            this.status = status;
            this.issuedAt = issuedAt;
            this.usedAt = usedAt;
        }
        
        public Long getUserCouponId() {
            return userCouponId;
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
        
        public LocalDateTime getUsedAt() {
            return usedAt;
        }
    }
} 