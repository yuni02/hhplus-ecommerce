package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.user.domain.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    private Long id;
    private Long userId;
    
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
    
    private BigDecimal totalAmount;
    private BigDecimal discountedAmount;
    private BigDecimal discountAmount;     
    private Long userCouponId;
    
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    
    private LocalDateTime orderedAt;
    private LocalDateTime updatedAt;
    private User user;
    
    @Builder.Default
    private List<OrderHistoryEvent> historyEvents = new ArrayList<>();

    // 비즈니스 로직 메서드들

    public void addHistoryEvent(OrderHistoryEvent event) {
        this.historyEvents.add(event);
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
        this.orderedAt = LocalDateTime.now();
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

    public void calculateDiscountedAmount() {
        if (totalAmount != null) {
            if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                this.discountedAmount = totalAmount.subtract(discountAmount);
            } else {    
                this.discountedAmount = totalAmount;
            }
        }
    }

    public BigDecimal getDiscountedAmount() {
        if (discountedAmount == null) {
            calculateDiscountedAmount();
        }
        return discountedAmount;
    }

    public Order(Long userId, List<OrderItem> orderItems, BigDecimal totalAmount, Long userCouponId, OrderStatus status, LocalDateTime orderedAt) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
        this.userCouponId = userCouponId;
        this.status = status;
        this.orderedAt = orderedAt;
    }

    public enum OrderStatus {
        PENDING, COMPLETED, CANCELLED, FAILED
    }
} 