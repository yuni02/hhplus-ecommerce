package kr.hhplus.be.server.product.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class ProductRankingUpdateEvent extends ApplicationEvent {
    
    private final Long productId;
    private final Integer quantity;
    private final LocalDateTime occurredAt;
    
    public ProductRankingUpdateEvent(Object source, Long productId, Integer quantity) {
        super(source);
        this.productId = productId;
        this.quantity = quantity;
        this.occurredAt = LocalDateTime.now();
    }
}