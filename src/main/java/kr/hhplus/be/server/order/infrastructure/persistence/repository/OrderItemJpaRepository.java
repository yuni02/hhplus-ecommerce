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
}