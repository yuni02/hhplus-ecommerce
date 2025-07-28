package kr.hhplus.be.server.order.adapter.out.persistence;

import kr.hhplus.be.server.order.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 복잡 쿼리용 QueryDSL Repository
 */
public interface OrderQueryRepository {

    /**
     * 사용자별 주문 통계 조회
     */
    List<Order> findUserOrderStats(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 금액대별 주문 조회
     */
    Page<Order> findOrdersByAmountRange(BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);

    /**
     * 쿠폰 사용 주문 조회
     */
    List<Order> findOrdersWithCouponUsage(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 복합 조건으로 주문 검색
     */
    Page<Order> findOrdersByComplexCondition(Long userId, String status, BigDecimal minAmount, 
                                           BigDecimal maxAmount, LocalDateTime startDate, 
                                           LocalDateTime endDate, Pageable pageable);
} 