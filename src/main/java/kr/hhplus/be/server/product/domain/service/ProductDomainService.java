package kr.hhplus.be.server.product.domain.service;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.StockDeductionRequestedEvent;
import kr.hhplus.be.server.order.domain.StockDeductionCompletedEvent;
import kr.hhplus.be.server.order.domain.StockRestorationRequestedEvent;
import kr.hhplus.be.server.order.domain.StockRestorationCompletedEvent;
import kr.hhplus.be.server.shared.event.SynchronousEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 상품 도메인 서비스
 * 재고 관련 비즈니스 로직을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductDomainService {
    
    private final SynchronousEventProcessor synchronousEventProcessor;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 재고 처리 (이벤트 기반 분산 트랜잭션)
     */
    public StockProcessResult processStockDeduction(CreateOrderUseCase.CreateOrderCommand command) {
        log.debug("재고 처리 시작 - userId: {}, itemCount: {}", 
                 command.getUserId(), command.getOrderItems().size());
        
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (CreateOrderUseCase.OrderItemCommand itemCommand : command.getOrderItems()) {
            // 재고 차감 이벤트 발행
            String requestId = "stock_" + itemCommand.getProductId() + "_" + System.currentTimeMillis();
            StockDeductionRequestedEvent requestEvent = new StockDeductionRequestedEvent(
                this, requestId, itemCommand.getProductId(), itemCommand.getQuantity()
            );
            
            try {
                // 동기 이벤트 처리
                StockDeductionCompletedEvent responseEvent = synchronousEventProcessor.publishAndWaitForResponse(
                    requestEvent, requestId, StockDeductionCompletedEvent.class, 5
                );
                
                if (!responseEvent.isSuccess()) {
                    log.warn("재고 차감 실패 - productId: {}, error: {}", 
                            itemCommand.getProductId(), responseEvent.getErrorMessage());
                    return StockProcessResult.failure(responseEvent.getErrorMessage());
                }
                
                OrderItem orderItem = createOrderItem(itemCommand.getProductId(), responseEvent.getProductName(),
                                                    itemCommand.getQuantity(), responseEvent.getUnitPrice());
                orderItems.add(orderItem);
                totalAmount = totalAmount.add(orderItem.getTotalPrice());
                
                log.debug("재고 차감 성공 - productId: {}, quantity: {}", 
                         itemCommand.getProductId(), itemCommand.getQuantity());
                
            } catch (Exception e) {
                log.error("재고 이벤트 처리 실패 - productId: {}", itemCommand.getProductId(), e);
                return StockProcessResult.failure("재고 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        
        log.debug("재고 처리 완료 - userId: {}, totalAmount: {}", command.getUserId(), totalAmount);
        return StockProcessResult.success(orderItems, totalAmount);
    }
    
    /**
     * 재고 복원 (보상 트랜잭션)
     */
    public void rollbackStockDeduction(List<OrderItem> orderItems, String reason) {
        log.warn("재고 복원 시작 - {} 개 아이템, 사유: {}", orderItems.size(), reason);
        
        for (OrderItem item : orderItems) {
            String requestId = "stock_rollback_" + item.getProductId() + "_" + System.currentTimeMillis();
            
            StockRestorationRequestedEvent rollbackEvent = new StockRestorationRequestedEvent(
                this, requestId, item.getProductId(), item.getQuantity(), reason
            );
            
            try {
                // 동기 이벤트 처리로 복원 진행
                StockRestorationCompletedEvent responseEvent = synchronousEventProcessor.publishAndWaitForResponse(
                    rollbackEvent, requestId, StockRestorationCompletedEvent.class, 3
                );
                
                if (responseEvent.isSuccess()) {
                    log.info("재고 복원 성공 - productId: {}, quantity: {}", 
                             item.getProductId(), item.getQuantity());
                } else {
                    log.error("재고 복원 실패 - productId: {}, error: {}", 
                              item.getProductId(), responseEvent.getErrorMessage());
                    // TODO: 재고 복원 실패 시 알림 또는 매뉴얼 처리 필요
                }
                
            } catch (Exception e) {
                log.error("재고 복원 이벤트 처리 실패 - productId: {}", item.getProductId(), e);
            }
        }
    }
    
    /**
     * 주문 아이템 생성
     */
    private OrderItem createOrderItem(Long productId, String productName, Integer quantity, BigDecimal unitPrice) {
        OrderItem orderItem = OrderItem.builder()
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .orderId(null) // 임시로 null 설정, Order 저장 후 업데이트됨
            .build();
        
        orderItem.calculateTotalPrice();
        return orderItem;
    }
    
    // 결과 클래스
    public static class StockProcessResult {
        private final boolean success;
        private final List<OrderItem> orderItems;
        private final BigDecimal totalAmount;
        private final String errorMessage;
        
        private StockProcessResult(boolean success, List<OrderItem> orderItems, 
                                 BigDecimal totalAmount, String errorMessage) {
            this.success = success;
            this.orderItems = orderItems;
            this.totalAmount = totalAmount;
            this.errorMessage = errorMessage;
        }
        
        public static StockProcessResult success(List<OrderItem> orderItems, BigDecimal totalAmount) {
            return new StockProcessResult(true, orderItems, totalAmount, null);
        }
        
        public static StockProcessResult failure(String errorMessage) {
            return new StockProcessResult(false, null, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public List<OrderItem> getOrderItems() { return orderItems; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getErrorMessage() { return errorMessage; }
    }
}