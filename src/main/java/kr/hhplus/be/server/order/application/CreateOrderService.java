package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.application.port.in.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.port.out.DeductBalancePort;
import kr.hhplus.be.server.order.application.port.out.LoadProductPort;
import kr.hhplus.be.server.order.application.port.out.LoadUserPort;
import kr.hhplus.be.server.order.application.port.out.SaveOrderPort;
import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;
import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;

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
    
    private final AtomicLong orderItemIdGenerator = new AtomicLong(1);

    public CreateOrderService(LoadUserPort loadUserPort,
                             LoadProductPort loadProductPort,
                             UpdateProductStockPort updateProductStockPort,
                             DeductBalancePort deductBalancePort,
                             SaveOrderPort saveOrderPort) {
        this.loadUserPort = loadUserPort;
        this.loadProductPort = loadProductPort;
        this.updateProductStockPort = updateProductStockPort;
        this.deductBalancePort = deductBalancePort;
        this.saveOrderPort = saveOrderPort;
    }

    @Override
    @Transactional
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        try {
            // 1. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return CreateOrderResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 2. 상품 검증 및 주문 아이템 생성
            List<OrderItem> orderItems = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (OrderItemCommand itemCommand : command.getOrderItems()) {
                // 상품 조회
                LoadProductPort.ProductInfo productInfo = loadProductPort.loadProductById(itemCommand.getProductId())
                        .orElse(null);
                
                if (productInfo == null) {
                    return CreateOrderResult.failure("존재하지 않는 상품입니다: " + itemCommand.getProductId());
                }

                // 재고 확인
                if (productInfo.getStock() < itemCommand.getQuantity()) {
                    return CreateOrderResult.failure("재고가 부족합니다: " + productInfo.getName());
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
                    return CreateOrderResult.failure("재고 차감에 실패했습니다: " + productInfo.getName());
                }
            }

            // 3. 쿠폰 할인 적용 (실제로는 쿠폰 도메인과 연동 필요)
            BigDecimal discountedAmount = totalAmount;
            if (command.getUserCouponId() != null) {
                // 쿠폰 할인 로직은 별도 구현 필요
                // discountedAmount = applyCouponDiscount(totalAmount, discountAmount);
            }

            // 4. 잔액 차감
            if (!deductBalancePort.deductBalance(command.getUserId(), discountedAmount)) {
                return CreateOrderResult.failure("잔액이 부족합니다.");
            }

            // 5. 주문 생성
            Order order = createOrder(command.getUserId(), orderItems, totalAmount, command.getUserCouponId());
            order.setDiscountedAmount(discountedAmount);

            // 6. 주문 아이템에 orderId 설정
            for (OrderItem item : orderItems) {
                item.setOrderId(order.getId());
            }

            // 7. 주문 완료 처리
            completeOrder(order);

            // 8. 주문 저장
            order = saveOrderPort.saveOrder(order);

            // 9. 결과 생성
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
        } catch (Exception e) {
            return CreateOrderResult.failure("주문 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 주문 아이템 생성 도메인 로직
     */
    private OrderItem createOrderItem(Long productId, String productName, Integer quantity, BigDecimal unitPrice) {
        OrderItem orderItem = new OrderItem(null, productId, productName, quantity, unitPrice);
        orderItem.setId(orderItemIdGenerator.getAndIncrement());
        return orderItem;
    }

    /**
     * 주문 생성 도메인 로직
     */
    private Order createOrder(Long userId, List<OrderItem> orderItems, BigDecimal totalAmount, Long userCouponId) {
        Order order = new Order(userId, orderItems, totalAmount, userCouponId);
        order.setOrderedAt(LocalDateTime.now());
        return order;
    }

    /**
     * 주문 완료 처리 도메인 로직
     */
    private void completeOrder(Order order) {
        order.complete();
    }
} 