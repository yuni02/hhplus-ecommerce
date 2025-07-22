package kr.hhplus.be.server.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 도메인 엔티티
 * 순수한 비즈니스 로직만 포함
 */
public class Order {

    private Long id;
    private Long userId;
    private List<OrderItem> orderItems = new ArrayList<>();
    private BigDecimal totalAmount;
    private BigDecimal discountedAmount;
    private Long userCouponId;
    private OrderStatus status = OrderStatus.PENDING;
    private LocalDateTime orderedAt;

    public Order() {}

    public Order(Long userId, List<OrderItem> orderItems, BigDecimal totalAmount, Long userCouponId) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
        this.userCouponId = userCouponId;
        this.orderedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscountedAmount() {
        return discountedAmount;
    }

    public void setDiscountedAmount(BigDecimal discountedAmount) {
        this.discountedAmount = discountedAmount;
    }

    public Long getUserCouponId() {
        return userCouponId;
    }

    public void setUserCouponId(Long userCouponId) {
        this.userCouponId = userCouponId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }

    public void addOrderItem(OrderItem item) {
        this.orderItems.add(item);
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public enum OrderStatus {
        PENDING, COMPLETED, CANCELLED, FAILED
    }
} 