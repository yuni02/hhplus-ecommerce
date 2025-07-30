package kr.hhplus.be.server.order.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 이력 이벤트 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 * 이벤트 소싱 및 감사 추적을 위한 로그성 테이블 (INSERT ONLY)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistoryEvent {

    private Long id; // 로그 고유 ID (불변)
    private Long orderId; // FK → Order
    private OrderEventType eventType; // ORDER_COMPLETED / CANCELLED / REFUNDED
    private LocalDateTime occurredAt; // 이벤트 발생 시각
    private String cancelReason; // 주문 취소 사유 (nullable)
    private Integer refundAmount; // 환불 금액 (nullable)
    private String paymentMethod; // 결제 수단 (nullable)
    private Integer totalAmount; // 주문 총액
    private Integer discountAmount; // 할인 금액
    private Integer finalAmount; // 최종 결제 금액
    private LocalDateTime createdAt; // 로그 생성 시각
    private Order order;

    /**
     * 주문 완료 이벤트 생성
     */
    public static OrderHistoryEvent orderCompleted(Long orderId, Integer totalAmount, 
                                                  Integer discountAmount, Integer finalAmount, 
                                                  String paymentMethod) {
        return OrderHistoryEvent.builder()
                .orderId(orderId)
                .eventType(OrderEventType.ORDER_COMPLETED)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .paymentMethod(paymentMethod)
                .occurredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 주문 취소 이벤트 생성
     */
    public static OrderHistoryEvent orderCancelled(Long orderId, String cancelReason, 
                                                  Integer totalAmount, Integer discountAmount) {
        return OrderHistoryEvent.builder()
                .orderId(orderId)
                .eventType(OrderEventType.CANCELLED)
                .cancelReason(cancelReason)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(0) // 취소된 주문의 최종 금액은 0
                .occurredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 환불 이벤트 생성
     */
    public static OrderHistoryEvent orderRefunded(Long orderId, Integer refundAmount, 
                                                 Integer totalAmount, String paymentMethod) {
        return OrderHistoryEvent.builder()
                .orderId(orderId)
                .eventType(OrderEventType.REFUNDED)
                .refundAmount(refundAmount)
                .totalAmount(totalAmount)
                .finalAmount(refundAmount)
                .paymentMethod(paymentMethod)
                .occurredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public enum OrderEventType {
        ORDER_COMPLETED, // 주문 완료
        CANCELLED,       // 주문 취소
        REFUNDED        // 환불 처리
    }
} 