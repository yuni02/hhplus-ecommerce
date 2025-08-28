package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.domain.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderProcessingState {
    private final String orderId;
    private final CreateOrderUseCase.CreateOrderCommand command;
    private final LocalDateTime startedAt;
    
    // 처리 단계별 상태
    private boolean stockProcessed = false;
    private boolean couponProcessed = false;
    private boolean balanceProcessed = false;
    private boolean orderSaved = false;
    
    // 처리 결과
    private List<OrderItem> orderItems;
    private BigDecimal totalAmount;
    private BigDecimal discountedAmount;
    private BigDecimal discountAmount;
    private String status = "PROCESSING";
    private String errorMessage;
    
    // 실패 시 rollback 정보
    private boolean stockRollbackNeeded = false;
    private boolean couponRollbackNeeded = false;
    private boolean balanceRollbackNeeded = false;

    public OrderProcessingState(String orderId, CreateOrderUseCase.CreateOrderCommand command) {
        this.orderId = orderId;
        this.command = command;
        this.startedAt = LocalDateTime.now();
    }
    
    public boolean isCompleted() {
        return stockProcessed && couponProcessed && balanceProcessed && orderSaved;
    }
    
    public boolean hasFailed() {
        return errorMessage != null;
    }
    
    public void markFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = "FAILED";
    }
    
    public void markCompleted() {
        this.status = "COMPLETED";
    }
}