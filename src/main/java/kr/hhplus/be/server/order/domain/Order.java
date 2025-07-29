package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
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
    private LocalDateTime updatedAt;
    private User user;
    private List<OrderHistoryEvent> historyEvents = new ArrayList<>();

    public Order() {
        this.orderItems = new ArrayList<>();
    }

    public Order(Long userId, List<OrderItem> orderItems, BigDecimal totalAmount, Long userCouponId) {
        this.userId = userId;
        this.orderItems = orderItems != null ? orderItems : new ArrayList<>();
        this.totalAmount = totalAmount;
        this.discountedAmount = totalAmount; // 기본값은 할인 없음
        this.userCouponId = userCouponId;
        this.orderedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // OrderItem들과의 관계 설정
        for (OrderItem item : this.orderItems) {
            item.setOrder(this);
        }
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
        // OrderItem들과의 관계 설정
        if (orderItems != null) {
            for (OrderItem item : orderItems) {
                item.setOrder(this);
            }
        }
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<OrderHistoryEvent> getHistoryEvents() {
        return historyEvents;
    }

    public void setHistoryEvents(List<OrderHistoryEvent> historyEvents) {
        this.historyEvents = historyEvents;
    }

    public void addOrderItem(OrderItem item) {
        this.orderItems.add(item);
        item.setOrder(this);
    }

    public void addHistoryEvent(OrderHistoryEvent event) {
        this.historyEvents.add(event);
        event.setOrder(this);
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
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