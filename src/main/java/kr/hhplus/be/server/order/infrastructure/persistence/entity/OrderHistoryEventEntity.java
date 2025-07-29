package kr.hhplus.be.server.order.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * OrderHistoryEvent 인프라스트럭처 엔티티
 * Order 도메인 전용 JPA 매핑 엔티티
 * 외래키 제약조건 없이 느슨한 결합으로 설계
 */
@Entity
@Table(name = "order_history_events")
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

    @Column(name = "discount_amount")
    private Integer discountAmount; // 할인 금액

    @Column(name = "final_amount", nullable = false)
    private Integer finalAmount; // 최종 결제 금액

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 로그 생성 시각

    public OrderHistoryEventEntity() {}

    public OrderHistoryEventEntity(Long orderId, String eventType, Integer totalAmount, 
                                  Integer discountAmount, Integer finalAmount, String paymentMethod) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.paymentMethod = paymentMethod;
        this.occurredAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Integer getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Integer refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Integer finalAmount) {
        this.finalAmount = finalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}