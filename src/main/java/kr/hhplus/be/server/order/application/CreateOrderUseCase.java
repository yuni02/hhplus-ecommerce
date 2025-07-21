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
    public Order execute(Long userId, List<OrderItemRequest> orderItems, Long userCouponId) {
        // 1. 사용자 검증
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 2. 상품 검증 및 주문 아이템 생성
        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : orderItems) {
            Optional<Product> productOpt = productRepository.findById(itemRequest.getProductId());
            if (productOpt.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 상품입니다: " + itemRequest.getProductId());
            }

            Product product = productOpt.get();
            
            // 상품 유효성 검증
            if (!ProductDomainService.isValidProduct(product)) {
                throw new IllegalArgumentException("유효하지 않은 상품입니다: " + product.getName());
            }

            // 재고 검증
            if (!ProductDomainService.hasSufficientStock(product, itemRequest.getQuantity())) {
                throw new IllegalArgumentException("재고가 부족합니다: " + product.getName());
            }

            // 주문 아이템 생성
            OrderItem orderItem = OrderDomainService.createOrderItem(
                    null, // orderId는 나중에 설정
                    product.getId(),
                    product.getName(),
                    itemRequest.getQuantity(),
                    product.getCurrentPrice()
            );

            items.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());

            // 재고 차감
            ProductDomainService.decreaseStock(product, itemRequest.getQuantity());
            productRepository.save(product);
        }

        // 3. 쿠폰 할인 적용 (실제로는 쿠폰 도메인과 연동 필요)
        BigDecimal discountedAmount = totalAmount;
        if (userCouponId != null) {
            // 쿠폰 할인 로직은 별도 구현 필요
            // discountedAmount = OrderDomainService.applyCouponDiscount(totalAmount, discountAmount);
        }

        // 4. 잔액 차감
        Balance balance = chargeBalanceUseCase.execute(userId, discountedAmount);

        // 5. 주문 생성
        Order order = OrderDomainService.createOrder(userId, items, totalAmount, userCouponId);
        order.setDiscountedAmount(discountedAmount);

        // 6. 주문 아이템에 orderId 설정
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
        }

        // 7. 주문 완료 처리
        OrderDomainService.completeOrder(order);

        // 8. 주문 저장
        return orderRepository.save(order);
    }

    // 내부 DTO 클래스
    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;

        public OrderItemRequest() {}

        public OrderItemRequest(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
} 