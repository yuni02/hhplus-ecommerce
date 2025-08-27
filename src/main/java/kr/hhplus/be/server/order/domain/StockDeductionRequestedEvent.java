package kr.hhplus.be.server.order.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StockDeductionRequestedEvent extends ApplicationEvent {
    
    private final String requestId;
    private final Long productId;
    private final Integer quantity;
    
    public StockDeductionRequestedEvent(Object source, String requestId, Long productId, Integer quantity) {
        super(source);
        this.requestId = requestId;
        this.productId = productId;
        this.quantity = quantity;
    }
}