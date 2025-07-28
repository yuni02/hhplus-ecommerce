package kr.hhplus.be.server.coupon.adapter.out.persistence;

import kr.hhplus.be.server.coupon.domain.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserCoupon 엔티티 JPA Repository
 */
@Repository
public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {

    /**
     * 사용자별 쿠폰 목록 조회
     */
    List<UserCoupon> findByUserId(Long userId);

    /**
     * 사용자별 사용 가능한 쿠폰 조회
     */
    List<UserCoupon> findByUserIdAndStatus(Long userId, UserCoupon.UserCouponStatus status);

    /**
     * 사용자가 특정 쿠폰을 보유하고 있는지 확인
     */
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.couponId = :couponId")
    List<UserCoupon> findByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 사용자의 사용 가능한 쿠폰 중 특정 쿠폰 조회
     */
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.id = :userCouponId AND uc.status = 'AVAILABLE'")
    Optional<UserCoupon> findAvailableUserCoupon(@Param("userId") Long userId, @Param("userCouponId") Long userCouponId);

    /**
     * 주문에 사용된 쿠폰 조회
     */
    Optional<UserCoupon> findByOrderId(Long orderId);
} 