package kr.hhplus.be.server.order.domain.event;

import kr.hhplus.be.server.order.domain.OrderItem;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터 플랫폼 전송 요청 이벤트
 * 주문 완료 후 데이터 플랫폼으로 주문 정보를 전송하기 위한 이벤트
 */
@Getter
public class DataPlatformTransferRequestedEvent extends ApplicationEvent {
    
    private final Long orderId;
    private final Long userId;
    private final List<OrderItem> orderItems;
    private final BigDecimal totalAmount;
    private final BigDecimal discountedAmount;
    private final BigDecimal discountAmount;
    private final Long userCouponId;
    private final LocalDateTime orderedAt;
    private final LocalDateTime occurredAt;
    
    public DataPlatformTransferRequestedEvent(Object source, Long orderId, Long userId,
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
    
    /**
     * OrderCompletedEvent에서 변환하여 생성
     */
    public static DataPlatformTransferRequestedEvent from(OrderCompletedEvent orderEvent) {
        return new DataPlatformTransferRequestedEvent(
            orderEvent.getSource(),
            orderEvent.getOrderId(),
            orderEvent.getUserId(),
            orderEvent.getOrderItems(),
            orderEvent.getTotalAmount(),
            orderEvent.getDiscountedAmount(),
            orderEvent.getDiscountAmount(),
            orderEvent.getUserCouponId(),
            orderEvent.getOrderedAt()
        );
    }
}