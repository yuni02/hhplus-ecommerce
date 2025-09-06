package kr.hhplus.be.server.unit.order.domain;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    @DisplayName("Order 생성 성공")
    void createOrder_Success() {
        // given
        Long userId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("20000");
        Long userCouponId = null;

        // when
        Order order = Order.builder()
            .userId(userId)
            .orderItems(orderItems)
            .totalAmount(totalAmount)
            .userCouponId(userCouponId)
            .build();

        // then
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getOrderItems()).isEqualTo(orderItems);
        assertThat(order.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(order.getUserCouponId()).isNull();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Order 생성 성공 - 쿠폰 사용")
    void createOrder_Success_WithCoupon() {
        // given
        Long userId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("20000");
        Long userCouponId = 1L;

        // when
        Order order = Order.builder()
            .userId(userId)
            .orderItems(orderItems)
            .totalAmount(totalAmount)
            .userCouponId(userCouponId)
            .build();

        // then
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getOrderItems()).isEqualTo(orderItems);
        assertThat(order.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(order.getUserCouponId()).isEqualTo(userCouponId);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Order 할인 금액 계산")
    void calculateDiscountedAmount() {
        // given
        BigDecimal totalAmount = new BigDecimal("20000");
        BigDecimal discountAmount = new BigDecimal("2000");
        Order order = Order.builder()
            .userId(1L)
            .orderItems(new ArrayList<>())
            .totalAmount(totalAmount)
            .discountAmount(discountAmount)
            .build();

        // when
        order.calculateDiscountedAmount();

        // then
        assertThat(order.getDiscountedAmount()).isEqualTo(new BigDecimal("18000"));
    }

    @Test
    @DisplayName("Order 할인 금액 계산 - 할인 없음")
    void calculateDiscountedAmount_NoDiscount() {
        // given
        BigDecimal totalAmount = new BigDecimal("20000");
        Order order = Order.builder()
            .userId(1L)
            .orderItems(new ArrayList<>())
            .totalAmount(totalAmount)
            .build();

        // when
        order.calculateDiscountedAmount();

        // then
        assertThat(order.getDiscountedAmount()).isEqualTo(totalAmount);
    }

    @Test
    @DisplayName("Order 완료 처리")
    void completeOrder() {
        // given
        Long userId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("20000");
        Long userCouponId = null;
        Order order = Order.builder()
            .userId(userId)
            .orderItems(orderItems)
            .totalAmount(totalAmount)
            .userCouponId(userCouponId)
            .build();

        // when
        order.complete();

        // then
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
        assertThat(order.getOrderedAt()).isNotNull();
    }

    @Test
    @DisplayName("Order 취소 처리")
    void cancelOrder() {
        // given
        Long userId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("20000");
        Long userCouponId = null;
        Order order = Order.builder()
            .userId(userId)
            .orderItems(orderItems)
            .totalAmount(totalAmount)
            .userCouponId(userCouponId)
            .build();

        // when
        order.cancel();

        // then
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Order 상태별 검증")
    void validateOrderStatuses() {
        // given & when & then
        assertThat(Order.OrderStatus.PENDING).isNotNull();
        assertThat(Order.OrderStatus.COMPLETED).isNotNull();
        assertThat(Order.OrderStatus.CANCELLED).isNotNull();
    }

    @Test
    @DisplayName("주문 완료 여부 확인")
    void isCompleted() {
        // given
        Order pendingOrder = Order.builder()
            .userId(1L)
            .orderItems(new ArrayList<>())
            .totalAmount(new BigDecimal("20000"))
            .status(Order.OrderStatus.PENDING)
            .build();

        Order completedOrder = Order.builder()
            .userId(1L)
            .orderItems(new ArrayList<>())
            .totalAmount(new BigDecimal("20000"))
            .status(Order.OrderStatus.COMPLETED)
            .build();

        // then
        assertThat(pendingOrder.isCompleted()).isFalse();
        assertThat(completedOrder.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("주문 취소 여부 확인")
    void isCancelled() {
        // given
        Order pendingOrder = Order.builder()
            .userId(1L)
            .orderItems(new ArrayList<>())
            .totalAmount(new BigDecimal("20000"))
            .status(Order.OrderStatus.PENDING)
            .build();

        Order cancelledOrder = Order.builder()
            .userId(1L)
            .orderItems(new ArrayList<>())
            .totalAmount(new BigDecimal("20000"))
            .status(Order.OrderStatus.CANCELLED)
            .build();

        // then
        assertThat(pendingOrder.isCancelled()).isFalse();
        assertThat(cancelledOrder.isCancelled()).isTrue();
    }
} 