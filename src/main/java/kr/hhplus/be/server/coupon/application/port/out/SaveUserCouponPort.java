package kr.hhplus.be.server.coupon.application.port.out;

import kr.hhplus.be.server.coupon.domain.UserCoupon;

/**
 * 사용자 쿠폰 저장 Outgoing Port
 */
public interface SaveUserCouponPort {
    
    /**
     * 사용자 쿠폰 저장
     */
    UserCoupon saveUserCoupon(UserCoupon userCoupon);
    

} 