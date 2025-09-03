package kr.hhplus.be.server.coupon.infrastructure.persistence.repository;

import kr.hhplus.be.server.coupon.infrastructure.persistence.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.util.List;
import java.util.Optional;

/**
 * Coupon 엔티티 JPA Repository
 * Coupon 도메인 전용 데이터 접근 계층
 */
@Repository
public interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {

    /**
     * 활성 상태인 쿠폰 목록 조회
     */
    List<CouponEntity> findByStatus(String status);  

    /** 
     * 발급 가능한 쿠폰 조회 (PESSIMISTIC 락 사용)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "1000")}) // 타임아웃 1초로 단축
    @Query("SELECT c FROM CouponEntity c WHERE c.id = :couponId")
    Optional<CouponEntity> findByIdWithLock(@Param("couponId") Long couponId);

    /**
     * 발급 가능한 쿠폰 조회 (OPTIMISTIC 락 사용 - 성능 향상)
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM CouponEntity c WHERE c.id = :couponId")
    Optional<CouponEntity> findByIdWithOptimisticLock(@Param("couponId") Long couponId);

    /**
     * 쿠폰 발급 수량 증가 (동시성 제어) - 개선된 버전
     */
    @Modifying
    @Query("UPDATE CouponEntity c SET c.issuedCount = c.issuedCount + 1, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :couponId AND c.issuedCount < c.maxIssuanceCount AND c.status = 'ACTIVE'")
    int incrementIssuedCount(@Param("couponId") Long couponId);

    /**
     * 쿠폰 발급 수량 증가 (배치 처리용)
     */
    @Modifying
    @Query("UPDATE CouponEntity c SET c.issuedCount = c.issuedCount + :increment, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :couponId AND c.issuedCount + :increment <= c.maxIssuanceCount AND c.status = 'ACTIVE'")
    int incrementIssuedCountByBatch(@Param("couponId") Long couponId, @Param("increment") int increment);
    
    /**
     * 쿠폰 발급 수량 감소 (롤백용)
     */
    @Modifying
    @Query("UPDATE CouponEntity c SET c.issuedCount = c.issuedCount - 1, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :couponId AND c.issuedCount > 0")
    int decrementIssuedCount(@Param("couponId") Long couponId);

    /**
     * 발급 가능한 쿠폰 조회
     */
    @Query("SELECT c FROM CouponEntity c WHERE c.status = :status AND c.issuedCount < c.maxIssuanceCount")
    List<CouponEntity> findAvailableCoupons(@Param("status") String status);
}