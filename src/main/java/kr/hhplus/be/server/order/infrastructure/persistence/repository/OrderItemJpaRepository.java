package kr.hhplus.be.server.order.infrastructure.persistence.repository;

import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * OrderItem 엔티티 JPA Repository
 * Order 도메인 전용 데이터 접근 계층
 */
@Repository
public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, Long> {

    /**
     * 주문별 주문 아이템 조회
     */
    List<OrderItemEntity> findByOrderId(Long orderId);

    /**
     * 상품별 주문 아이템 조회
     */
    List<OrderItemEntity> findByProductId(Long productId);

    /**
     * 특정 상품의 총 판매 수량 조회
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItemEntity oi WHERE oi.productId = :productId")
    Integer getTotalSoldQuantityByProductId(@Param("productId") Long productId);

    /**
     * 주문과 상품으로 주문 아이템 조회
     */
    List<OrderItemEntity> findByOrderIdAndProductId(Long orderId, Long productId);

    /**
     * 최근 3일간 상품별 판매 통계 조회
     */
    @Query("""
        SELECT oi.productId, oi.productName, 
               SUM(oi.quantity) as totalQuantity, 
               SUM(oi.totalPrice) as totalAmount,
               MAX(o.orderedAt) as lastOrderDate
        FROM OrderItemEntity oi
        JOIN OrderEntity o ON oi.orderId = o.id
        WHERE o.orderedAt BETWEEN :startDate AND :endDate
        AND o.status = 'COMPLETED'
        GROUP BY oi.productId, oi.productName
        ORDER BY totalQuantity DESC
        """)
    List<Object[]> findProductSalesStatsByDateRange(@Param("startDate") java.time.LocalDateTime startDate, 
                                                   @Param("endDate") java.time.LocalDateTime endDate);
}