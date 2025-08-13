package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.*;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadProductPort loadProductPort;
    private final UpdateProductStockPort updateProductStockPort;
    private final DeductBalancePort deductBalancePort;
    private final SaveOrderPort saveOrderPort;
    private final UseCouponUseCase useCouponUseCase;
    private final OrderDistributedLockService orderDistributedLockService;

    @Transactional
    public CreateOrderUseCase.CreateOrderResult createOrder(CreateOrderUseCase.CreateOrderCommand command) {
        List<OrderItem> createdOrderItems = null;
        boolean stockDeducted = false;

        try {
            // 1. 주문 검증
            OrderValidationResult validationResult = validateOrder(command);
            if (!validationResult.isValid()) {
                return CreateOrderUseCase.CreateOrderResult.failure(validationResult.getErrorMessage());
            }

            // 2. 주문 아이템 생성 및 재고 차감 (분산락 적용)
            OrderItemsResult itemsResult = createOrderItemsWithDistributedLock(command);
            if (!itemsResult.isSuccess()) {
                return CreateOrderUseCase.CreateOrderResult.failure(itemsResult.getErrorMessage());
            }
            createdOrderItems = itemsResult.getOrderItems();
            stockDeducted = true; // 재고 차감 완료 표시

            // 3. 쿠폰 할인 적용 (분산락 적용)
            CouponDiscountResult discountResult = applyCouponDiscountWithDistributedLock(command, itemsResult.getTotalAmount());
            if (!discountResult.isSuccess()) {
                // 쿠폰 할인 실패 시 재고 복구
                restoreStockWithDistributedLock(createdOrderItems);
                return CreateOrderUseCase.CreateOrderResult.failure(discountResult.getErrorMessage());
            }

            // 4. 잔액 차감 (분산락 적용)
            if (!orderDistributedLockService.deductBalanceWithDistributedLock(command.getUserId(), discountResult.getDiscountedAmount())) {
                // 잔액 부족 시 재고 복구
                restoreStockWithDistributedLock(createdOrderItems);
                return CreateOrderUseCase.CreateOrderResult.failure("잔액이 부족합니다.");
            }

            // 5. 주문 생성 및 저장
            Order order = createAndSaveOrder(command, createdOrderItems,
                                           itemsResult.getTotalAmount(), discountResult);

            // 6. 결과 반환
            return createOrderResult(order, createdOrderItems);

        } catch (Exception e) {
            // 예외 발생 시 재고 복구
            if (stockDeducted && createdOrderItems != null) {
                restoreStockWithDistributedLock(createdOrderItems);
            }
            return CreateOrderUseCase.CreateOrderResult.failure("주문 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 주문 검증
     */
    private OrderValidationResult validateOrder(CreateOrderUseCase.CreateOrderCommand command) {
        // 1. 입력값 검증
        if (command.getUserId() == null || command.getUserId() <= 0) {
            return OrderValidationResult.failure("잘못된 사용자 ID입니다.");
        }
        
        if (command.getOrderItems() == null || command.getOrderItems().isEmpty()) {
            return OrderValidationResult.failure("주문 상품이 없습니다.");
        }
        
        // 2. 주문 아이템 수량 검증
        for (CreateOrderUseCase.OrderItemCommand itemCommand : command.getOrderItems()) {
            if (itemCommand.getQuantity() <= 0) {
                return OrderValidationResult.failure("주문 수량은 1개 이상이어야 합니다.");
            }
        }
        
        // 3. 사용자 존재 확인
        if (!loadUserPort.existsById(command.getUserId())) {
            return OrderValidationResult.failure("사용자를 찾을 수 없습니다.");
        }
        return OrderValidationResult.success();
    }

    /**
     * 주문 아이템 생성 및 재고 차감 (분산락 적용)
     */
    private OrderItemsResult createOrderItemsWithDistributedLock(CreateOrderUseCase.CreateOrderCommand command) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderUseCase.OrderItemCommand itemCommand : command.getOrderItems()) {
            // 상품 정보 조회
            LoadProductPort.ProductInfo productInfo = loadProductPort.loadProductById(itemCommand.getProductId())
                .orElse(null);
            
            if (productInfo == null) {
                return OrderItemsResult.failure("상품을 찾을 수 없습니다: " + itemCommand.getProductId());
            }
            
            // 재고 확인
            if (productInfo.getStock() < itemCommand.getQuantity()) {
                return OrderItemsResult.failure("재고가 부족합니다: " + productInfo.getName());
            }
            
            // 재고 차감 (분산락 적용)
            if (!orderDistributedLockService.deductProductStockWithDistributedLock(itemCommand.getProductId(), itemCommand.getQuantity())) {
                return OrderItemsResult.failure("재고 차감에 실패했습니다: " + productInfo.getName());
            }
            
            // 주문 아이템 생성
            OrderItem orderItem = createOrderItem(itemCommand.getProductId(), productInfo.getName(), 
                                                itemCommand.getQuantity(), productInfo.getCurrentPrice());
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }
        
        return OrderItemsResult.success(orderItems, totalAmount);
    }

    /**
     * 쿠폰 할인 적용 (분산락 적용)
     */
    private CouponDiscountResult applyCouponDiscountWithDistributedLock(CreateOrderUseCase.CreateOrderCommand command, BigDecimal totalAmount) {
        if (command.getUserCouponId() == null) {
            return CouponDiscountResult.success(totalAmount, 0);
        }

        UseCouponUseCase.UseCouponCommand couponCommand = 
            new UseCouponUseCase.UseCouponCommand(command.getUserId(), command.getUserCouponId(), totalAmount);
        
        UseCouponUseCase.UseCouponResult couponResult = orderDistributedLockService.applyCouponDiscountWithFairLock(couponCommand);
        
        if (!couponResult.isSuccess()) {
            return CouponDiscountResult.failure(couponResult.getErrorMessage());
        }
        
        return CouponDiscountResult.success(couponResult.getDiscountedAmount(), couponResult.getDiscountAmount());
    }

    /**
     * 재고 복구 (여러 상품)
     */
    private void restoreStockWithDistributedLock(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            try {
                orderDistributedLockService.restoreProductStockWithDistributedLock(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to restore stock for product: {}", item.getProductId(), e);
            }
        }
    }

    /**
     * 주문 생성 및 저장
     */
    private Order createAndSaveOrder(CreateOrderUseCase.CreateOrderCommand command, 
                                   List<OrderItem> orderItems, 
                                   BigDecimal totalAmount, 
                                   CouponDiscountResult discountResult) {
        Order order = Order.builder()
            .userId(command.getUserId())
            .orderItems(orderItems)
            .totalAmount(totalAmount)
            .userCouponId(command.getUserCouponId())
            .discountedAmount(discountResult.getDiscountedAmount())
            .discountAmount(BigDecimal.valueOf(discountResult.getDiscountAmount()))
            .orderedAt(LocalDateTime.now())
            .status(Order.OrderStatus.PENDING)
            .build();
        
        // finalAmount 계산
        order.calculateDiscountedAmount();   
        order.complete();

        // Order 저장
        Order savedOrder = saveOrderPort.saveOrder(order);
        
        // OrderItem들의 orderId 업데이트
        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getId());
        }
        
        return savedOrder;
    }

    /**
     * 주문 결과 생성
     */
    private CreateOrderUseCase.CreateOrderResult createOrderResult(Order order, List<OrderItem> orderItems) {
        List<CreateOrderUseCase.OrderItemResult> orderItemResults = new ArrayList<>();
        for (OrderItem item : orderItems) {
            orderItemResults.add(new CreateOrderUseCase.OrderItemResult(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
            ));
        }

        return CreateOrderUseCase.CreateOrderResult.success(
            order.getId(),
            order.getUserId(),
            order.getUserCouponId(),
            order.getTotalAmount(),
            order.getDiscountedAmount(),
            order.getDiscountAmount(),
            order.getStatus().name(),
            orderItemResults,
            order.getOrderedAt()
        );
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
        
        // totalPrice 계산
        orderItem.calculateTotalPrice();
        
        return orderItem;
    }

    // 내부 클래스들
    private static class OrderValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private OrderValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static OrderValidationResult success() {
            return new OrderValidationResult(true, null);
        }
        
        public static OrderValidationResult failure(String errorMessage) {
            return new OrderValidationResult(false, errorMessage);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }

    private static class OrderItemsResult {
        private final boolean success;
        private final List<OrderItem> orderItems;
        private final BigDecimal totalAmount;
        private final String errorMessage;
        
        private OrderItemsResult(boolean success, List<OrderItem> orderItems, BigDecimal totalAmount, String errorMessage) {
            this.success = success;
            this.orderItems = orderItems;
            this.totalAmount = totalAmount;
            this.errorMessage = errorMessage;
        }
        
        public static OrderItemsResult success(List<OrderItem> orderItems, BigDecimal totalAmount) {
            return new OrderItemsResult(true, orderItems, totalAmount, null);
        }
        
        public static OrderItemsResult failure(String errorMessage) {
            return new OrderItemsResult(false, null, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public List<OrderItem> getOrderItems() { return orderItems; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getErrorMessage() { return errorMessage; }
    }

    private static class CouponDiscountResult {
        private final boolean success;
        private final BigDecimal discountedAmount;
        private final Integer discountAmount;
        private final String errorMessage;
        
        private CouponDiscountResult(boolean success, BigDecimal discountedAmount, Integer discountAmount, String errorMessage) {
            this.success = success;
            this.discountedAmount = discountedAmount;
            this.discountAmount = discountAmount;
            this.errorMessage = errorMessage;
        }
        
        public static CouponDiscountResult success(BigDecimal discountedAmount, Integer discountAmount) {
            return new CouponDiscountResult(true, discountedAmount, discountAmount, null);
        }
        
        public static CouponDiscountResult failure(String errorMessage) {
            return new CouponDiscountResult(false, null, null, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public BigDecimal getDiscountedAmount() { return discountedAmount; }
        public Integer getDiscountAmount() { return discountAmount; }
        public String getErrorMessage() { return errorMessage; }
    }
}
