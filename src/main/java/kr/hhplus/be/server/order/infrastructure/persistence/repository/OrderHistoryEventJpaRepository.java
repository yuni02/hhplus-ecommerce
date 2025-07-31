package kr.hhplus.be.server.order.infrastructure.persistence.repository;

import kr.hhplus.be.server.order.infrastructure.persistence.entity.OrderHistoryEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderHistoryEvent 엔티티 JPA Repository
 * Order 도메인 전용 데이터 접근 계층
 * 이벤트 소싱 및 감사 추적을 위한 로그성 테이블 (INSERT ONLY)
 */
@Repository
public interface OrderHistoryEventJpaRepository extends JpaRepository<OrderHistoryEventEntity, Long> {

    /**
     * 주문별 이벤트 내역 조회 (시간순 정렬)
     */
    @Query("SELECT ohe FROM OrderHistoryEventEntity ohe WHERE ohe.orderId = :orderId ORDER BY ohe.occurredAt ASC")
    List<OrderHistoryEventEntity> findByOrderIdOrderByOccurredAt(@Param("orderId") Long orderId);

    /**
     * 특정 이벤트 타입으로 조회
     */
    List<OrderHistoryEventEntity> findByEventType(String eventType);

    /**
     * 특정 기간 내 이벤트 조회
     */
    @Query("SELECT ohe FROM OrderHistoryEventEntity ohe WHERE ohe.occurredAt BETWEEN :startDate AND :endDate ORDER BY ohe.occurredAt DESC")
    List<OrderHistoryEventEntity> findByOccurredAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                         @Param("endDate") LocalDateTime endDate);

    /**
     * 주문의 최신 이벤트 조회
     */
    @Query("SELECT ohe FROM OrderHistoryEventEntity ohe WHERE ohe.orderId = :orderId ORDER BY ohe.occurredAt DESC LIMIT 1")
    OrderHistoryEventEntity findLatestEventByOrderId(@Param("orderId") Long orderId);

    /**
     * 특정 주문의 완료 이벤트 조회
     */
    @Query("SELECT ohe FROM OrderHistoryEventEntity ohe WHERE ohe.orderId = :orderId AND ohe.eventType = 'ORDER_COMPLETED'")
    List<OrderHistoryEventEntity> findCompletedEventsByOrderId(@Param("orderId") Long orderId);

    /**
     * 환불 이벤트 조회
     */
    @Query("SELECT ohe FROM OrderHistoryEventEntity ohe WHERE ohe.eventType = 'REFUNDED' ORDER BY ohe.occurredAt DESC")
    List<OrderHistoryEventEntity> findRefundEvents();

    /**
     * 취소 이벤트 조회
     */
    @Query("SELECT ohe FROM OrderHistoryEventEntity ohe WHERE ohe.eventType = 'CANCELLED' ORDER BY ohe.occurredAt DESC")
    List<OrderHistoryEventEntity> findCancelledEvents();

    /**
     * 특정 결제 수단으로 완료된 주문 이벤트 조회
     */
    @Query("SELECT ohe FROM OrderHistoryEventEntity ohe WHERE ohe.paymentMethod = :paymentMethod AND ohe.eventType = 'ORDER_COMPLETED'")
    List<OrderHistoryEventEntity> findCompletedEventsByPaymentMethod(@Param("paymentMethod") String paymentMethod);
}