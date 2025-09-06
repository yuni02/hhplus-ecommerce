package kr.hhplus.be.server.order.domain.event;

import kr.hhplus.be.server.order.domain.OrderItem;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderCompletedEvent extends ApplicationEvent {
    
    private final Long orderId;
    private final Long userId;
    private final List<OrderItem> orderItems;
    private final BigDecimal totalAmount;
    private final BigDecimal discountedAmount;
    private final BigDecimal discountAmount;
    private final Long userCouponId;
    private final LocalDateTime orderedAt;
    private final LocalDateTime occurredAt;
    
    public OrderCompletedEvent(Object source, Long orderId, Long userId, 
                             List<OrderItem> orderItems, BigDecimal totalAmount,
                             BigDecimal discountedAmount, BigDecimal discountAmount,
                             Long userCouponId, LocalDateTime orderedAt) {
        super(source);
        this.orderId = orderId;
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
        this.discountedAmount = discountedAmount;
        this.discountAmount = discountAmount;
        this.userCouponId = userCouponId;
        this.orderedAt = orderedAt;
        this.occurredAt = LocalDateTime.now();
    }
}
