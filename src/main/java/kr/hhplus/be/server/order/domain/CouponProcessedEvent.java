package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public class CouponProcessedEvent extends ApplicationEvent {
    private final String eventId;
    private final CreateOrderUseCase.CreateOrderCommand command;
    private final List<OrderItem> orderItems;
    private final BigDecimal discountedAmount;
    private final Integer discountAmount;
    private final LocalDateTime occurredAt;

    public CouponProcessedEvent(Object source, CreateOrderUseCase.CreateOrderCommand command,
                               List<OrderItem> orderItems, BigDecimal discountedAmount, Integer discountAmount) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.command = command;
        this.orderItems = orderItems;
        this.discountedAmount = discountedAmount;
        this.discountAmount = discountAmount;
        this.occurredAt = LocalDateTime.now();
    }
}