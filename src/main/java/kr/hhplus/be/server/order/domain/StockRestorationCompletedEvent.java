package kr.hhplus.be.server.order.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StockRestorationCompletedEvent extends ApplicationEvent {
    
    private final String requestId;
    private final Long productId;
    private final Integer quantity;
    private final boolean success;
    private final String errorMessage;
    
    private StockRestorationCompletedEvent(Object source, String requestId, Long productId, 
                                         Integer quantity, boolean success, String errorMessage) {
        super(source);
        this.requestId = requestId;
        this.productId = productId;
        this.quantity = quantity;
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public static StockRestorationCompletedEvent success(Object source, String requestId, 
                                                       Long productId, Integer quantity) {
        return new StockRestorationCompletedEvent(source, requestId, productId, quantity, true, null);
    }
    
    public static StockRestorationCompletedEvent failure(Object source, String requestId, 
                                                       Long productId, Integer quantity, String errorMessage) {
        return new StockRestorationCompletedEvent(source, requestId, productId, quantity, false, errorMessage);
    }
}