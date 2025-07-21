package kr.hhplus.be.server.coupon.domain;

import java.time.LocalDateTime;

/**
 * 순수한 쿠폰 도메인 로직만 포함하는 도메인 서비스
 * 프레임워크나 외부 의존성 없음
 */
public class CouponDomainService {

    /**
     * 쿠폰 발급 가능 여부 확인 도메인 로직
     */
    public static boolean canIssueCoupon(Coupon coupon) {
        return coupon.canIssue();
    }

    /**
     * 쿠폰 발급 처리 도메인 로직
     */
    public static Coupon issueCoupon(Coupon coupon) {
        if (!canIssueCoupon(coupon)) {
            throw new IllegalStateException("쿠폰을 발급할 수 없습니다.");
        }
        
        coupon.incrementIssuedCount();
        return coupon;
    }

    /**
     * 사용자 쿠폰 생성 도메인 로직
     */
    public static UserCoupon createUserCoupon(Long userId, Long couponId) {
        return new UserCoupon(userId, couponId);
    }

    /**
     * 쿠폰 사용 가능 여부 확인 도메인 로직
     */
    public static boolean canUseCoupon(UserCoupon userCoupon) {
        return userCoupon.isAvailable();
    }

    /**
     * 쿠폰 사용 처리 도메인 로직
     */
    public static UserCoupon useCoupon(UserCoupon userCoupon, Long orderId) {
        if (!canUseCoupon(userCoupon)) {
            throw new IllegalStateException("사용할 수 없는 쿠폰입니다.");
        }
        
        userCoupon.use(orderId);
        return userCoupon;
    }

    /**
     * 쿠폰 유효성 검증 도메인 로직
     */
    public static boolean isValidCoupon(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        return coupon.getStatus() == Coupon.CouponStatus.ACTIVE &&
               now.isAfter(coupon.getValidFrom()) &&
               now.isBefore(coupon.getValidTo());
    }

    /**
     * 중복 발급 확인 도메인 로직
     */
    public static boolean isAlreadyIssued(Long userId, Long couponId, boolean existsByUserIdAndCouponId) {
        return existsByUserIdAndCouponId;
    }
} 