package kr.hhplus.be.server.order.infrastructure.persistence.repository;

import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;      

/**
 * Order 엔티티 JPA Repository
 * Order 도메인 전용 데이터 접근 계층
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * 사용자별 주문 목록 조회
     */
    List<OrderEntity> findByUserId(Long userId);

    /**
     * 사용자별 특정 상태의 주문 조회
     */
    List<OrderEntity> findByUserIdAndStatus(Long userId, String status);

    /**
     * 특정 기간 내 주문 조회
     */
    List<OrderEntity> findByOrderedAtBetweenOrderByOrderedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 쿠폰을 사용한 주문 조회
     */
    List<OrderEntity> findByUserCouponId(Long userCouponId);

    /**
     * 사용자의 최근 주문 조회
     */
    List<OrderEntity> findByUserIdOrderByOrderedAtDesc(Long userId);
}