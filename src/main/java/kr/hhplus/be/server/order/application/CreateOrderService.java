package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.*;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.shared.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    /**
     * 단일 분산락으로 주문 생성
     * 사용자별로 하나의 락을 사용하여 동시 주문 방지 및 부분 실패 방지
     */
    @DistributedLock(
        key = "'order-creation:' + #command.userId",
        waitTime = 10,
        leaseTime = 30,
        timeUnit = TimeUnit.SECONDS
    )
    @Transactional
    public CreateOrderUseCase.CreateOrderResult createOrder(CreateOrderUseCase.CreateOrderCommand command) {
        log.debug("주문 생성 시작 (단일 분산락) - userId: {}", command.getUserId());
        
        try {
            // 1. 주문 검증
            if (!validateOrder(command)) {
                return CreateOrderUseCase.CreateOrderResult.failure("주문 검증에 실패했습니다.");
            }

            // 2. 재고 확인 및 차감
            OrderItemsResult itemsResult = processStockDeduction(command);
            if (!itemsResult.isSuccess()) {
                return CreateOrderUseCase.CreateOrderResult.failure(itemsResult.getErrorMessage());
            }

            // 3. 쿠폰 할인 적용
            CouponDiscountResult discountResult = processCouponDiscount(command, itemsResult.getTotalAmount());
            if (!discountResult.isSuccess()) {
                return CreateOrderUseCase.CreateOrderResult.failure(discountResult.getErrorMessage());
            }

            // 4. 잔액 차감
            if (!processBalanceDeduction(command.getUserId(), discountResult.getDiscountedAmount())) {
                return CreateOrderUseCase.CreateOrderResult.failure("잔액이 부족합니다.");
            }

            // 5. 주문 생성 및 저장
            Order order = createAndSaveOrder(command, itemsResult.getOrderItems(), 
                                           itemsResult.getTotalAmount(), discountResult);

            // 6. 성공 결과 반환
            CreateOrderUseCase.CreateOrderResult result = createOrderResult(order, itemsResult.getOrderItems());
            log.debug("주문 생성 완료 (단일 분산락) - userId: {}, orderId: {}", command.getUserId(), order.getId());
            
            return result;

        } catch (Exception e) {
            log.error("주문 생성 중 예외 발생 (단일 분산락) - userId: {}", command.getUserId(), e);
            return CreateOrderUseCase.CreateOrderResult.failure("주문 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 주문 검증
     */
    private boolean validateOrder(CreateOrderUseCase.CreateOrderCommand command) {
        // 1. 입력값 검증
        if (command.getUserId() == null || command.getUserId() <= 0) {
            return false;
        }
        
        if (command.getOrderItems() == null || command.getOrderItems().isEmpty()) {
            return false;
        }
        
        // 2. 주문 아이템 수량 검증
        for (CreateOrderUseCase.OrderItemCommand itemCommand : command.getOrderItems()) {
            if (itemCommand.getQuantity() <= 0) {
                return false;
            }
        }
        
        // 3. 사용자 존재 확인
        return loadUserPort.existsById(command.getUserId());
    }

    /**
     * 재고 처리 (비관적 락 사용)
     */
    private OrderItemsResult processStockDeduction(CreateOrderUseCase.CreateOrderCommand command) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderUseCase.OrderItemCommand itemCommand : command.getOrderItems()) {
            LoadProductPort.ProductInfo productInfo = loadProductPort.loadProductById(itemCommand.getProductId())
                .orElse(null);
            
            if (productInfo == null) {
                return OrderItemsResult.failure("상품을 찾을 수 없습니다: " + itemCommand.getProductId());
            }
            
            if (productInfo.getStock() < itemCommand.getQuantity()) {
                return OrderItemsResult.failure("재고가 부족합니다: " + productInfo.getName());
            }
            
            // 비관적 락으로 재고 차감
            if (!updateProductStockPort.deductStockWithPessimisticLock(itemCommand.getProductId(), itemCommand.getQuantity())) {
                return OrderItemsResult.failure("재고 차감에 실패했습니다: " + productInfo.getName());
            }
            
            OrderItem orderItem = createOrderItem(itemCommand.getProductId(), productInfo.getName(), 
                                                itemCommand.getQuantity(), productInfo.getCurrentPrice());
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }
        
        return OrderItemsResult.success(orderItems, totalAmount);
    }

    /**
     * 쿠폰 할인 처리 (비관적 락 사용)
     */
    private CouponDiscountResult processCouponDiscount(CreateOrderUseCase.CreateOrderCommand command, BigDecimal totalAmount) {
        if (command.getUserCouponId() == null) {
            return CouponDiscountResult.success(totalAmount, 0);
        }

        UseCouponUseCase.UseCouponCommand couponCommand = 
            new UseCouponUseCase.UseCouponCommand(command.getUserId(), command.getUserCouponId(), totalAmount);
        
        UseCouponUseCase.UseCouponResult couponResult = useCouponUseCase.useCouponWithPessimisticLock(couponCommand);
        
        if (!couponResult.isSuccess()) {
            return CouponDiscountResult.failure(couponResult.getErrorMessage());
        }
        
        return CouponDiscountResult.success(couponResult.getDiscountedAmount(), couponResult.getDiscountAmount());
    }

    /**
     * 잔액 차감 처리 (비관적 락 사용)
     */
    private boolean processBalanceDeduction(Long userId, BigDecimal amount) {
        return deductBalancePort.deductBalanceWithPessimisticLock(userId, amount);
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
        private final int discountAmount;
        private final String errorMessage;
        
        private CouponDiscountResult(boolean success, BigDecimal discountedAmount, int discountAmount, String errorMessage) {
            this.success = success;
            this.discountedAmount = discountedAmount;
            this.discountAmount = discountAmount;
            this.errorMessage = errorMessage;
        }
        
        public static CouponDiscountResult success(BigDecimal discountedAmount, int discountAmount) {
            return new CouponDiscountResult(true, discountedAmount, discountAmount, null);
        }
        
        public static CouponDiscountResult failure(String errorMessage) {
            return new CouponDiscountResult(false, null, 0, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public BigDecimal getDiscountedAmount() { return discountedAmount; }
        public int getDiscountAmount() { return discountAmount; }
        public String getErrorMessage() { return errorMessage; }
    }
}
