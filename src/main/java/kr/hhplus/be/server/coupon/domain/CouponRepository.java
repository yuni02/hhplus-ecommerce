package kr.hhplus.be.server.coupon.domain;

import java.util.Optional;

public interface CouponRepository {
    
    Optional<Coupon> findById(Long id);
    
    Coupon save(Coupon coupon);
    
    boolean existsById(Long id);
} 