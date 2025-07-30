package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.order.application.port.out.LoadUserPort;
import kr.hhplus.be.server.order.application.port.out.SaveOrderPort;
import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 주문 생성 Application 서비스
 */
@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadProductPort loadProductPort;
    private final UpdateProductStockPort updateProductStockPort;
    private final DeductBalancePort deductBalancePort;
    private final SaveOrderPort saveOrderPort;
    private final UseCouponUseCase useCouponUseCase;
    
    private final AtomicLong orderItemIdGenerator = new AtomicLong(1);

    public CreateOrderService(LoadUserPort loadUserPort,
                             LoadProductPort loadProductPort,
                             UpdateProductStockPort updateProductStockPort,
                             DeductBalancePort deductBalancePort,
                             SaveOrderPort saveOrderPort,
                             UseCouponUseCase useCouponUseCase) {
        this.loadUserPort = loadUserPort;
        this.loadProductPort = loadProductPort;
        this.updateProductStockPort = updateProductStockPort;
        this.deductBalancePort = deductBalancePort;
        this.saveOrderPort = saveOrderPort;
        this.useCouponUseCase = useCouponUseCase;
    }

    @Override
    @Transactional
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        List<OrderItem> createdOrderItems = null;
        boolean stockDeducted = false;
        
        try {
            // 1. 주문 검증
            OrderValidationResult validationResult = validateOrder(command);
            if (!validationResult.isValid()) {
                return CreateOrderResult.failure(validationResult.getErrorMessage());
            }

            // 2. 주문 아이템 생성 및 재고 차감
            OrderItemsResult itemsResult = createOrderItems(command);
            if (!itemsResult.isSuccess()) {
                return CreateOrderResult.failure(itemsResult.getErrorMessage());
            }
            createdOrderItems = itemsResult.getOrderItems();
            stockDeducted = true; // 재고 차감 완료 표시

            // 3. 쿠폰 할인 적용
            CouponDiscountResult discountResult = applyCouponDiscount(command, itemsResult.getTotalAmount());
            if (!discountResult.isSuccess()) {
                // 쿠폰 할인 실패 시 재고 복구
                restoreStock(createdOrderItems);
                return CreateOrderResult.failure(discountResult.getErrorMessage());
            }

            // 4. 잔액 차감
            if (!deductBalancePort.deductBalance(command.getUserId(), discountResult.getDiscountedAmount())) {
                // 잔액 부족 시 재고 복구
                restoreStock(createdOrderItems);
                return CreateOrderResult.failure("잔액이 부족합니다.");
            }

            // 5. 주문 생성 및 저장
            Order order = createAndSaveOrder(command, createdOrderItems, 
                                           itemsResult.getTotalAmount(), discountResult.getDiscountedAmount());

            // 6. 결과 반환
            return createOrderResult(order, createdOrderItems);

        } catch (Exception e) {
            // 예외 발생 시 재고 복구
            if (stockDeducted && createdOrderItems != null) {
                restoreStock(createdOrderItems);
            }
            return CreateOrderResult.failure("주문 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 주문 검증
     */
    private OrderValidationResult validateOrder(CreateOrderCommand command) {
        // 1. 입력값 검증
        if (command.getUserId() == null || command.getUserId() <= 0) {
            return OrderValidationResult.failure("잘못된 사용자 ID입니다.");
        }
        
        if (command.getOrderItems() == null || command.getOrderItems().isEmpty()) {
            return OrderValidationResult.failure("주문 상품이 없습니다.");
        }
        
        // 2. 사용자 존재 확인
        if (!loadUserPort.existsById(command.getUserId())) {
            return OrderValidationResult.failure("사용자를 찾을 수 없습니다.");
        }
        return OrderValidationResult.success();
    }

    /**
     * 주문 아이템 생성 및 재고 차감
     */
    private OrderItemsResult createOrderItems(CreateOrderCommand command) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemCommand itemCommand : command.getOrderItems()) {
            // 상품 조회
            LoadProductPort.ProductInfo productInfo = loadProductPort.loadProductById(itemCommand.getProductId())
                    .orElse(null);
            
            if (productInfo == null) {
                return OrderItemsResult.failure("상품을 찾을 수 없습니다: " + itemCommand.getProductId());
            }

            // 재고 확인
            if (productInfo.getStock() < itemCommand.getQuantity()) {
                return OrderItemsResult.failure("재고가 부족합니다: " + productInfo.getStock());
            }

            // 주문 아이템 생성
            OrderItem orderItem = createOrderItem(
                    productInfo.getId(),
                    productInfo.getName(),
                    itemCommand.getQuantity(),
                    productInfo.getCurrentPrice()
            );

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());

            // 재고 차감
            if (!updateProductStockPort.deductStock(itemCommand.getProductId(), itemCommand.getQuantity())) {
                return OrderItemsResult.failure("재고 차감에 실패했습니다: " + productInfo.getName());
            }
        }

        return OrderItemsResult.success(orderItems, totalAmount);
    }

    /**
     * 쿠폰 할인 적용
     */
    private CouponDiscountResult applyCouponDiscount(CreateOrderCommand command, BigDecimal totalAmount) {
        if (command.getUserCouponId() == null) {
            return CouponDiscountResult.success(totalAmount, 0);
        }

        UseCouponUseCase.UseCouponCommand couponCommand = 
            new UseCouponUseCase.UseCouponCommand(command.getUserId(), command.getUserCouponId(), totalAmount);
        
        UseCouponUseCase.UseCouponResult couponResult = useCouponUseCase.useCoupon(couponCommand);
        
        if (!couponResult.isSuccess()) {
            return CouponDiscountResult.failure(couponResult.getErrorMessage());
        }
        
        return CouponDiscountResult.success(couponResult.getDiscountedAmount(), couponResult.getDiscountAmount());
    }

    /**
     * 주문 생성 및 저장
     */
    private Order createAndSaveOrder(CreateOrderCommand command, 
                                   List<OrderItem> orderItems, 
                                   BigDecimal totalAmount, 
                                   BigDecimal discountedAmount) {
        Order order = Order.builder()
            .userId(command.getUserId())
            .orderItems(orderItems)
            .totalAmount(totalAmount)
            .userCouponId(command.getUserCouponId())
            .discountedAmount(discountedAmount)
            .orderedAt(LocalDateTime.now())
            .status(Order.OrderStatus.PENDING)
            .build();
        order.complete();

        // 주문 아이템에 orderId 설정
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }

        return saveOrderPort.saveOrder(order);
    }

    /**
     * 주문 결과 생성
     */
    private CreateOrderResult createOrderResult(Order order, List<OrderItem> orderItems) {
        List<OrderItemResult> orderItemResults = new ArrayList<>();
        for (OrderItem item : orderItems) {
            orderItemResults.add(new OrderItemResult(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
            ));
        }

        return CreateOrderResult.success(
            order.getId(),
            order.getUserId(),
            order.getUserCouponId(),
            order.getTotalAmount(),
            order.getDiscountedAmount(),
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
            .build();
        orderItem.setId(orderItemIdGenerator.getAndIncrement());    
        return orderItem;
    }

    /**
     * 재고 복구 메서드
     */
    private void restoreStock(List<OrderItem> orderItems) {
        try {
            for (OrderItem item : orderItems) {
                // 재고 복구 (차감된 수량만큼 다시 증가)
                updateProductStockPort.restoreStock(item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            // 재고 복구 실패 시 로그 기록 (운영팀 알림 필요)
            System.err.println("재고 복구 실패: " + e.getMessage());
            // TODO: 운영팀 알림 로직 추가
        }
    }

    // 내부 결과 클래스들
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