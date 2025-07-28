package kr.hhplus.be.server.order.adapter.out.persistence;

import kr.hhplus.be.server.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Order 엔티티 JPA Repository
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    /**
     * 사용자별 주문 목록 조회
     */
    List<Order> findByUserId(Long userId);

    /**
     * 사용자별 특정 상태의 주문 조회
     */
    List<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status);

    /**
     * 특정 기간 내 주문 조회
     */
    List<Order> findByOrderedAtBetweenOrderByOrderedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 쿠폰을 사용한 주문 조회
     */
    List<Order> findByUserCouponId(Long userCouponId);

    /**
     * 사용자의 최근 주문 조회
     */
    List<Order> findByUserIdOrderByOrderedAtDesc(Long userId);
} 