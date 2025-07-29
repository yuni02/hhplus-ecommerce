package kr.hhplus.be.server.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 이력 이벤트 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 * 이벤트 소싱 및 감사 추적을 위한 로그성 테이블 (INSERT ONLY)
 */
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

    public OrderHistoryEvent() {}

    /**
     * 주문 완료 이벤트 생성
     */
    public static OrderHistoryEvent orderCompleted(Long orderId, Integer totalAmount, 
                                                  Integer discountAmount, Integer finalAmount, 
                                                  String paymentMethod) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.orderId = orderId;
        event.eventType = OrderEventType.ORDER_COMPLETED;
        event.totalAmount = totalAmount;
        event.discountAmount = discountAmount;
        event.finalAmount = finalAmount;
        event.paymentMethod = paymentMethod;
        event.occurredAt = LocalDateTime.now();
        event.createdAt = LocalDateTime.now();
        return event;
    }

    /**
     * 주문 취소 이벤트 생성
     */
    public static OrderHistoryEvent orderCancelled(Long orderId, String cancelReason, 
                                                  Integer totalAmount, Integer discountAmount) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.orderId = orderId;
        event.eventType = OrderEventType.CANCELLED;
        event.cancelReason = cancelReason;
        event.totalAmount = totalAmount;
        event.discountAmount = discountAmount;
        event.finalAmount = 0; // 취소된 주문의 최종 금액은 0
        event.occurredAt = LocalDateTime.now();
        event.createdAt = LocalDateTime.now();
        return event;
    }

    /**
     * 환불 이벤트 생성
     */
    public static OrderHistoryEvent orderRefunded(Long orderId, Integer refundAmount, 
                                                 Integer totalAmount, String paymentMethod) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.orderId = orderId;
        event.eventType = OrderEventType.REFUNDED;
        event.refundAmount = refundAmount;
        event.totalAmount = totalAmount;
        event.finalAmount = refundAmount;
        event.paymentMethod = paymentMethod;
        event.occurredAt = LocalDateTime.now();
        event.createdAt = LocalDateTime.now();
        return event;
    }

    // Getters and Setters
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

    public OrderEventType getEventType() {
        return eventType;
    }

    public void setEventType(OrderEventType eventType) {
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

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    /**
     * 주문 이벤트 타입
     */
    public enum OrderEventType {
        ORDER_COMPLETED, // 주문 완료
        CANCELLED,       // 주문 취소
        REFUNDED        // 환불 처리
    }
} 