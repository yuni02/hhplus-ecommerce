package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.balance.domain.BalanceService;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderDomainService;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.OrderRepository;
import kr.hhplus.be.server.order.domain.OrderService;
import kr.hhplus.be.server.order.domain.OrderValidationResult;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductService;
import kr.hhplus.be.server.product.domain.ProductValidationResult;
import kr.hhplus.be.server.product.domain.StockDeductResult;
import kr.hhplus.be.server.balance.domain.BalanceDeductResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 주문 생성 UseCase
 * 여러 도메인 서비스를 조합하여 사용
 */
@Component
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ProductService productService;
    private final BalanceService balanceService;

    public CreateOrderUseCase(OrderRepository orderRepository,
                             OrderService orderService,
                             ProductService productService,
                             BalanceService balanceService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.productService = productService;
        this.balanceService = balanceService;
    }

    @Transactional
    public Output execute(Input input) {
        // 1. 주문 생성 검증
        OrderValidationResult validationResult = orderService.validateOrderCreation(input.userId, null);
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(validationResult.getErrorMessage());
        }

        // 2. 상품 검증 및 주문 아이템 생성
        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemInput itemInput : input.orderItems) {
            // 상품 검증
            ProductValidationResult productValidation = productService.validateProduct(itemInput.productId, itemInput.quantity);
            if (!productValidation.isValid()) {
                throw new IllegalArgumentException(productValidation.getErrorMessage());
            }

            Product product = productValidation.getProduct();

            // 주문 아이템 생성
            OrderItem orderItem = OrderDomainService.createOrderItem(
                    null, // orderId는 나중에 설정
                    product.getId(),
                    product.getName(),
                    itemInput.quantity,
                    product.getCurrentPrice()
            );

            items.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());

            // 재고 차감
            StockDeductResult stockResult = productService.deductStock(itemInput.productId, itemInput.quantity);
            if (!stockResult.isSuccess()) {
                throw new IllegalArgumentException(stockResult.getErrorMessage());
            }
        }

        // 3. 쿠폰 할인 적용 (실제로는 쿠폰 도메인과 연동 필요)
        BigDecimal discountedAmount = totalAmount;
        if (input.userCouponId != null) {
            // 쿠폰 할인 로직은 별도 구현 필요
            // discountedAmount = orderService.applyCouponDiscount(totalAmount, discountAmount);
        }

        // 4. 잔액 차감
        BalanceDeductResult balanceResult = balanceService.deductBalance(input.userId, discountedAmount);
        if (!balanceResult.isSuccess()) {
            throw new IllegalArgumentException(balanceResult.getErrorMessage());
        }

        // 5. 주문 생성
        Order order = OrderDomainService.createOrder(input.userId, items, totalAmount, input.userCouponId);
        order.setDiscountedAmount(discountedAmount);

        // 6. 주문 아이템에 orderId 설정
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
        }

        // 7. 주문 완료 처리
        OrderDomainService.completeOrder(order);

        // 8. 주문 저장
        order = orderRepository.save(order);

        // 9. Output 생성
        List<OrderItemOutput> orderItemOutputs = new ArrayList<>();
        for (OrderItem item : items) {
            orderItemOutputs.add(new OrderItemOutput(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
            ));
        }

        return new Output(
            order.getId(),
            order.getUserId(),
            order.getUserCouponId(),
            order.getTotalAmount().intValue(),
            order.getDiscountedAmount().intValue(),
            order.getStatus().name(),
            orderItemOutputs,
            order.getCreatedAt()
        );
    }

    public static class Input {
        private final Long userId;
        private final List<OrderItemInput> orderItems;
        private final Long userCouponId;

        public Input(Long userId, List<OrderItemInput> orderItems, Long userCouponId) {
            this.userId = userId;
            this.orderItems = orderItems;
            this.userCouponId = userCouponId;
        }

        public Long getUserId() {
            return userId;
        }

        public List<OrderItemInput> getOrderItems() {
            return orderItems;
        }

        public Long getUserCouponId() {
            return userCouponId;
        }
    }

    public static class OrderItemInput {
        private final Long productId;
        private final Integer quantity;

        public OrderItemInput(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public Integer getQuantity() {
            return quantity;
        }
    }

    public static class Output {
        private final Long id;
        private final Long userId;
        private final Long userCouponId;
        private final Integer totalPrice;
        private final Integer discountedPrice;
        private final String status;
        private final List<OrderItemOutput> orderItems;
        private final LocalDateTime createdAt;

        public Output(Long id, Long userId, Long userCouponId, Integer totalPrice, 
                     Integer discountedPrice, String status, List<OrderItemOutput> orderItems, 
                     LocalDateTime createdAt) {
            this.id = id;
            this.userId = userId;
            this.userCouponId = userCouponId;
            this.totalPrice = totalPrice;
            this.discountedPrice = discountedPrice;
            this.status = status;
            this.orderItems = orderItems;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getUserCouponId() {
            return userCouponId;
        }

        public Integer getTotalPrice() {
            return totalPrice;
        }

        public Integer getDiscountedPrice() {
            return discountedPrice;
        }

        public String getStatus() {
            return status;
        }

        public List<OrderItemOutput> getOrderItems() {
            return orderItems;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    public static class OrderItemOutput {
        private final Long id;
        private final Long productId;
        private final String productName;
        private final Integer quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal totalPrice;

        public OrderItemOutput(Long id, Long productId, String productName, 
                              Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }

        public Long getId() {
            return id;
        }

        public Long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }
    }
} 