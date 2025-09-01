package kr.hhplus.be.server.order.domain.service;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.LoadUserPort;
import kr.hhplus.be.server.order.application.port.out.SaveOrderPort;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.DataPlatformTransferRequestedEvent;
import kr.hhplus.be.server.product.domain.ProductRankingUpdateEvent;
import kr.hhplus.be.server.shared.event.AsyncEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주문 도메인 서비스
 * 주문과 관련된 비즈니스 로직을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDomainService {
    
    private final LoadUserPort loadUserPort;
    private final SaveOrderPort saveOrderPort;
    private final AsyncEventPublisher asyncEventPublisher;
    
    /**
     * 주문 검증
     */
    public OrderValidationResult validateOrder(CreateOrderUseCase.CreateOrderCommand command) {
        log.debug("주문 검증 시작 - userId: {}", command.getUserId());
        
        // 1. 입력값 검증
        if (command.getUserId() == null || command.getUserId() <= 0) {
            return OrderValidationResult.failure("유효하지 않은 사용자 ID입니다.");
        }
        
        if (command.getOrderItems() == null || command.getOrderItems().isEmpty()) {
            return OrderValidationResult.failure("주문 아이템이 없습니다.");
        }
        
        // 2. 주문 아이템 수량 검증
        for (CreateOrderUseCase.OrderItemCommand itemCommand : command.getOrderItems()) {
            if (itemCommand.getQuantity() <= 0) {
                return OrderValidationResult.failure("유효하지 않은 주문 수량입니다.");
            }
        }
        
        // 3. 사용자 존재 확인
        if (!loadUserPort.existsById(command.getUserId())) {
            return OrderValidationResult.failure("존재하지 않는 사용자입니다.");
        }
        
        log.debug("주문 검증 완료 - userId: {}", command.getUserId());
        return OrderValidationResult.success();
    }
    
    /**
     * 주문 생성 및 저장 (트랜잭션)
     */
    @Transactional
    public OrderCreationResult createAndSaveOrder(CreateOrderUseCase.CreateOrderCommand command,
                                                 List<OrderItem> orderItems,
                                                 BigDecimal totalAmount,
                                                 BigDecimal discountedAmount,
                                                 BigDecimal discountAmount) {
        log.debug("주문 생성 시작 - userId: {}", command.getUserId());
        
        try {
            Order order = Order.builder()
                .userId(command.getUserId())
                .orderItems(orderItems)
                .totalAmount(totalAmount)
                .userCouponId(command.getUserCouponId())
                .discountedAmount(discountedAmount)
                .discountAmount(discountAmount)
                .orderedAt(LocalDateTime.now())
                .status(Order.OrderStatus.PENDING)
                .build();
            
            order.calculateDiscountedAmount();
            order.complete();
            
            // Order 저장
            Order savedOrder = saveOrderPort.saveOrder(order);
            
            // OrderItem들의 orderId 업데이트 - Builder로 새 객체 생성
            List<OrderItem> updatedOrderItems = orderItems.stream()
                    .map(item -> item.toBuilder().orderId(savedOrder.getId()).build())
                    .toList();
            
            // 비동기 이벤트 발행
            publishAsyncEvents(savedOrder, orderItems);
            
            log.debug("주문 생성 완료 - userId: {}, orderId: {}", command.getUserId(), savedOrder.getId());
            return OrderCreationResult.success(savedOrder);
            
        } catch (Exception e) {
            log.error("주문 생성 중 예외 발생 - userId: {}", command.getUserId(), e);
            return OrderCreationResult.failure("주문 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 비동기 이벤트 발행
     */
    private void publishAsyncEvents(Order savedOrder, List<OrderItem> orderItems) {
        // 데이터 플랫폼 전송 이벤트 발행
        DataPlatformTransferRequestedEvent dataPlatformEvent = new DataPlatformTransferRequestedEvent(
            this,
            savedOrder.getId(),
            savedOrder.getUserId(),
            savedOrder.getOrderItems(),
            savedOrder.getTotalAmount(),
            savedOrder.getDiscountedAmount(),
            savedOrder.getDiscountAmount(),
            savedOrder.getUserCouponId(),
            savedOrder.getOrderedAt()
        );
        asyncEventPublisher.publishAsync(dataPlatformEvent, "data-platform-transfer");
        
        // 상품 랭킹 업데이트 이벤트 발행
        for (OrderItem item : orderItems) {
            ProductRankingUpdateEvent rankingEvent = new ProductRankingUpdateEvent(this, item.getProductId(), item.getQuantity());
            asyncEventPublisher.publishAsync(rankingEvent, "product-ranking");
        }
    }
    
    // 결과 클래스들
    public static class OrderValidationResult {
        private final boolean success;
        private final String errorMessage;
        
        private OrderValidationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static OrderValidationResult success() {
            return new OrderValidationResult(true, null);
        }
        
        public static OrderValidationResult failure(String errorMessage) {
            return new OrderValidationResult(false, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class OrderCreationResult {
        private final boolean success;
        private final Order order;
        private final String errorMessage;
        
        private OrderCreationResult(boolean success, Order order, String errorMessage) {
            this.success = success;
            this.order = order;
            this.errorMessage = errorMessage;
        }
        
        public static OrderCreationResult success(Order order) {
            return new OrderCreationResult(true, order, null);
        }
        
        public static OrderCreationResult failure(String errorMessage) {
            return new OrderCreationResult(false, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public Order getOrder() { return order; }
        public String getErrorMessage() { return errorMessage; }
    }
}