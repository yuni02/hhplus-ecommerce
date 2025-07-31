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
    public void addOrderItem(OrderItem item) {
        this.orderItems.add(item);
        item.setOrder(this);
    }

    public void addHistoryEvent(OrderHistoryEvent event) {
        this.historyEvents.add(event);
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

    public void calculateFinalAmount() {
        if (totalAmount != null) {
            if (discountedAmount != null && discountedAmount.compareTo(BigDecimal.ZERO) > 0) {
                this.discountAmount = totalAmount.subtract(discountedAmount);
            } else {    
                this.discountAmount = totalAmount;
            }
        }
    }

    public BigDecimal getFinalAmount() {
        if (discountAmount == null) {
            calculateFinalAmount();
        }
        return discountAmount;
    }

    public enum OrderStatus {
        PENDING, COMPLETED, CANCELLED, FAILED
    }
} 