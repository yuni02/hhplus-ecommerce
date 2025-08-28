package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class OrderValidatedEvent extends ApplicationEvent {
    private final String eventId;
    private final String orderId;
    private final CreateOrderUseCase.CreateOrderCommand command;
    private final LocalDateTime occurredAt;

    public OrderValidatedEvent(Object source, String orderId, CreateOrderUseCase.CreateOrderCommand command) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.command = command;
        this.occurredAt = LocalDateTime.now();
    }
}