package kr.hhplus.be.server.order.infrastructure.persistence.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.shared.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 히스토리 이벤트 전용 엔티티
 * INSERT ONLY 테이블로 설계 (이벤트 소싱용)
 */
@Entity
@Table(name = "order_history_events")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistoryEventEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private OrderEntity order;

    @Column(name = "order_id", nullable = false, insertable = false, updatable = false)
    private Long orderId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "discounted_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal discountedAmount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    // 비즈니스 메서드들
    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}