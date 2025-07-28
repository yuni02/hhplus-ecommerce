package kr.hhplus.be.server.order.adapter.out.persistence;

import kr.hhplus.be.server.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * OrderItem 엔티티 JPA Repository
 */
@Repository
public interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 주문별 주문 아이템 조회
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 상품별 주문 아이템 조회
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * 특정 상품의 총 판매 수량 조회
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.productId = :productId")
    Integer getTotalSoldQuantityByProductId(@Param("productId") Long productId);

    /**
     * 주문과 상품으로 주문 아이템 조회
     */
    List<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);
} 