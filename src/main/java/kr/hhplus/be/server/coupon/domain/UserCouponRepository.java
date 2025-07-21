package kr.hhplus.be.server.coupon.domain;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {
    
    List<UserCoupon> findByUserId(Long userId);
    
    List<UserCoupon> findByUserIdAndStatus(Long userId, UserCoupon.UserCouponStatus status);
    
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
    
    UserCoupon save(UserCoupon userCoupon);
    
    Optional<UserCoupon> findById(Long id);
} 