package kr.hhplus.be.server.order.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StockRestorationRequestedEvent extends ApplicationEvent {
    
    private final String requestId;
    private final Long productId;
    private final Integer quantity;
    private final String reason;
    
    public StockRestorationRequestedEvent(Object source, String requestId, Long productId, 
                                        Integer quantity, String reason) {
        super(source);
        this.requestId = requestId;
        this.productId = productId;
        this.quantity = quantity;
        this.reason = reason;
    }
}