package kr.hhplus.be.server.coupon.adapter.out.persistence;

import kr.hhplus.be.server.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Coupon 엔티티 JPA Repository
 */
@Repository
public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    /**
     * 활성 상태인 쿠폰 목록 조회
     */
    List<Coupon> findByStatus(Coupon.CouponStatus status);

    /**
     * 발급 가능한 쿠폰 조회 (락 사용)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :couponId")
    Optional<Coupon> findByIdWithLock(@Param("couponId") Long couponId);

    /**
     * 쿠폰 발급 수량 증가 (동시성 제어)
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.issuedCount = c.issuedCount + 1, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :couponId AND c.issuedCount < c.maxIssuanceCount AND c.status = 'ACTIVE'")
    int incrementIssuedCount(@Param("couponId") Long couponId);

    /**
     * 발급 가능한 쿠폰 조회
     */
    @Query("SELECT c FROM Coupon c WHERE c.status = :status AND c.issuedCount < c.maxIssuanceCount")
    List<Coupon> findAvailableCoupons(@Param("status") Coupon.CouponStatus status);
} 