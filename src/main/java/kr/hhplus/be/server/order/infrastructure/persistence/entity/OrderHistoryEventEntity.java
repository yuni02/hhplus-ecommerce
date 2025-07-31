package kr.hhplus.be.server.order.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * OrderHistoryEvent 인프라스트럭처 엔티티
 * Order 도메인 전용 JPA 매핑 엔티티
 * 외래키 제약조건 없이 느슨한 결합으로 설계
 */
@Entity
@Table(name = "order_history_events")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistoryEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id; // 로그 고유 ID (불변)

    @Column(name = "order_id", nullable = false)
    private Long orderId; // 외래키 제약조건 없음

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // enum 대신 varchar

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt; // 이벤트 발생 시각

    @Column(name = "cancel_reason")
    private String cancelReason; // 주문 취소 사유 (nullable)

    @Column(name = "refund_amount")
    private Integer refundAmount; // 환불 금액 (nullable)

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // 결제 수단 (nullable)

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount; // 주문 총액

    @Column(name = "discounted_amount")
    private Integer discountedAmount; // 할인 금액

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount; // 할인 금액

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 로그 생성 시각

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    // 필요한 경우에만 public setter 제공
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}