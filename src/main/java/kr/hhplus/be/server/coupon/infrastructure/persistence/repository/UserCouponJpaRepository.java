package kr.hhplus.be.server.coupon.infrastructure.persistence.repository;

import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.UserCouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserCoupon 엔티티 JPA Repository
 * Coupon 도메인 전용 데이터 접근 계층
 */
@Repository
public interface UserCouponJpaRepository extends JpaRepository<UserCouponEntity, Long> {

    /**
     * 사용자별 쿠폰 목록 조회
     */
    List<UserCouponEntity> findByUserId(Long userId);

    /**
     * 사용자별 사용 가능한 쿠폰 조회
     */
    List<UserCouponEntity> findByUserIdAndStatus(Long userId, String status);

    /**
     * 사용자가 특정 쿠폰을 보유하고 있는지 확인
     */
    @Query("SELECT uc FROM UserCouponEntity uc WHERE uc.userId = :userId AND uc.couponId = :couponId")
    List<UserCouponEntity> findByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 사용자의 사용 가능한 쿠폰 중 특정 쿠폰 조회
     */
    @Query("SELECT uc FROM UserCouponEntity uc WHERE uc.userId = :userId AND uc.id = :userCouponId AND uc.status = 'AVAILABLE'")
    Optional<UserCouponEntity> findAvailableUserCoupon(@Param("userId") Long userId, @Param("userCouponId") Long userCouponId);

    /**
     * 주문에 사용된 쿠폰 조회
     */
    Optional<UserCouponEntity> findByOrderId(Long orderId);
}