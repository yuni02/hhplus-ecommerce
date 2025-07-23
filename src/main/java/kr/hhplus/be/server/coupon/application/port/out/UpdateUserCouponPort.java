package kr.hhplus.be.server.coupon.application.port.out;

import kr.hhplus.be.server.coupon.domain.UserCoupon;

/**
 * 사용자 쿠폰 업데이트 Outgoing Port
 */
public interface UpdateUserCouponPort {
    
    /**
     * 사용자 쿠폰 업데이트
     */
    void updateUserCoupon(UserCoupon userCoupon);
} 