package kr.hhplus.be.server.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 순수한 주문 도메인 로직만 포함하는 도메인 서비스
 * 프레임워크나 외부 의존성 없음
 */
public class OrderDomainService {

    /**
     * 주문 생성 도메인 로직
     */
    public static Order createOrder(Long userId, List<OrderItem> orderItems, BigDecimal totalAmount, Long userCouponId) {
        Order order = new Order(userId, orderItems, totalAmount, userCouponId);
        order.setDiscountedAmount(totalAmount); // 기본값은 총액과 동일
        order.setOrderedAt(LocalDateTime.now());
        return order;
    }

    /**
     * 주문 아이템 생성 도메인 로직
     */
    public static OrderItem createOrderItem(Long orderId, Long productId, String productName, Integer quantity, BigDecimal unitPrice) {
        return new OrderItem(orderId, productId, productName, quantity, unitPrice);
    }

    /**
     * 주문 완료 처리 도메인 로직
     */
    public static Order completeOrder(Order order) {
        order.complete();
        return order;
    }

    /**
     * 주문 취소 처리 도메인 로직
     */
    public static Order cancelOrder(Order order) {
        order.cancel();
        return order;
    }

    /**
     * 주문 총액 계산 도메인 로직
     */
    public static BigDecimal calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 쿠폰 할인 적용 도메인 로직
     */
    public static BigDecimal applyCouponDiscount(BigDecimal totalAmount, BigDecimal discountAmount) {
        return totalAmount.subtract(discountAmount).max(BigDecimal.ZERO);
    }

    /**
     * 주문 유효성 검증 도메인 로직
     */
    public static boolean isValidOrder(Order order) {
        return order != null && 
               order.getUserId() != null && 
               order.getUserId() > 0 &&
               order.getOrderItems() != null && 
               !order.getOrderItems().isEmpty() &&
               order.getTotalAmount() != null && 
               order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 주문 아이템 유효성 검증 도메인 로직
     */
    public static boolean isValidOrderItem(OrderItem orderItem) {
        return orderItem != null && 
               orderItem.getProductId() != null && 
               orderItem.getProductId() > 0 &&
               orderItem.getQuantity() != null && 
               orderItem.getQuantity() > 0 &&
               orderItem.getUnitPrice() != null && 
               orderItem.getUnitPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 주문 상태 확인 도메인 로직
     */
    public static boolean canCompleteOrder(Order order) {
        return order != null && order.getStatus() == Order.OrderStatus.PENDING;
    }

    /**
     * 주문 취소 가능 여부 확인 도메인 로직
     */
    public static boolean canCancelOrder(Order order) {
        return order != null && 
               (order.getStatus() == Order.OrderStatus.PENDING || 
                order.getStatus() == Order.OrderStatus.COMPLETED);
    }
} 