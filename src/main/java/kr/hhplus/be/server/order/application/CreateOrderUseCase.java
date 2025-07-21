package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.balance.application.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.coupon.application.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderDomainService;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.OrderRepository;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductDomainService;
import kr.hhplus.be.server.product.domain.ProductRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserRepository;
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
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ChargeBalanceUseCase chargeBalanceUseCase;

    public CreateOrderUseCase(OrderRepository orderRepository,
                             ProductRepository productRepository,
                             UserRepository userRepository,
                             ChargeBalanceUseCase chargeBalanceUseCase) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.chargeBalanceUseCase = chargeBalanceUseCase;
    }

    @Transactional
    public Output execute(Input input) {
        // 1. 사용자 검증
        Optional<User> userOpt = userRepository.findById(input.userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 2. 상품 검증 및 주문 아이템 생성
        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemInput itemInput : input.orderItems) {
            Optional<Product> productOpt = productRepository.findById(itemInput.productId);
            if (productOpt.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 상품입니다: " + itemInput.productId);
            }

            Product product = productOpt.get();
            
            // 상품 유효성 검증
            if (!ProductDomainService.isValidProduct(product)) {
                throw new IllegalArgumentException("유효하지 않은 상품입니다: " + product.getName());
            }

            // 재고 검증
            if (!ProductDomainService.hasSufficientStock(product, itemInput.quantity)) {
                throw new IllegalArgumentException("재고가 부족합니다: " + product.getName());
            }

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
            ProductDomainService.decreaseStock(product, itemInput.quantity);
            productRepository.save(product);
        }

        // 3. 쿠폰 할인 적용 (실제로는 쿠폰 도메인과 연동 필요)
        BigDecimal discountedAmount = totalAmount;
        if (input.userCouponId != null) {
            // 쿠폰 할인 로직은 별도 구현 필요
            // discountedAmount = OrderDomainService.applyCouponDiscount(totalAmount, discountAmount);
        }

        // 4. 잔액 차감
        ChargeBalanceUseCase.Output balanceOutput = chargeBalanceUseCase.execute(new ChargeBalanceUseCase.Input(input.userId, discountedAmount));

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