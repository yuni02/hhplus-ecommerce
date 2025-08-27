package kr.hhplus.be.server.order.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class StockDeductionCompletedEvent extends ApplicationEvent {
    
    private final String requestId;
    private final Long productId;
    private final Integer quantity;
    private final boolean success;
    private final String productName;
    private final BigDecimal unitPrice;
    private final String errorMessage;
    
    private StockDeductionCompletedEvent(Object source, String requestId, Long productId, Integer quantity, 
                                       boolean success, String productName, BigDecimal unitPrice, String errorMessage) {
        super(source);
        this.requestId = requestId;
        this.productId = productId;
        this.quantity = quantity;
        this.success = success;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.errorMessage = errorMessage;
    }
    
    public static StockDeductionCompletedEvent success(Object source, String requestId, Long productId, 
                                                     Integer quantity, String productName, BigDecimal unitPrice) {
        return new StockDeductionCompletedEvent(source, requestId, productId, quantity, 
                                              true, productName, unitPrice, null);
    }
    
    public static StockDeductionCompletedEvent failure(Object source, String requestId, Long productId, 
                                                     Integer quantity, String errorMessage) {
        return new StockDeductionCompletedEvent(source, requestId, productId, quantity, 
                                              false, null, null, errorMessage);
    }
}