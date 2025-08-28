package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class OrderProcessingFailedEvent extends ApplicationEvent {
    private final String eventId;
    private final CreateOrderUseCase.CreateOrderCommand command;
    private final String errorMessage;
    private final LocalDateTime occurredAt;

    public OrderProcessingFailedEvent(Object source, CreateOrderUseCase.CreateOrderCommand command, String errorMessage) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.command = command;
        this.errorMessage = errorMessage;
        this.occurredAt = LocalDateTime.now();
    }
}