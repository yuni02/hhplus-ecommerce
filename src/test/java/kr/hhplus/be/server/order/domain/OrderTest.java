package kr.hhplus.be.server.order.domain;

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
        Order order = new Order(userId, orderItems, totalAmount, userCouponId);

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
        Order order = new Order(userId, orderItems, totalAmount, userCouponId);

        // then
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getOrderItems()).isEqualTo(orderItems);
        assertThat(order.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(order.getUserCouponId()).isEqualTo(userCouponId);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Order ID 설정")
    void setOrderId() {
        // given
        Long userId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("20000");
        Long userCouponId = null;
        Order order = new Order(userId, orderItems, totalAmount, userCouponId);
        
        Long orderId = 1L;

        // when
        order.setId(orderId);

        // then
        assertThat(order.getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Order 할인 금액 설정")
    void setOrderDiscountedAmount() {
        // given
        Long userId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("20000");
        Long userCouponId = 1L;
        Order order = new Order(userId, orderItems, totalAmount, userCouponId);
        
        BigDecimal discountedAmount = new BigDecimal("18000");

        // when
        order.setDiscountedAmount(discountedAmount);

        // then
        assertThat(order.getDiscountedAmount()).isEqualTo(discountedAmount);
    }

    @Test
    @DisplayName("Order 상태 변경")
    void changeOrderStatus() {
        // given
        Long userId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("20000");
        Long userCouponId = null;
        Order order = new Order(userId, orderItems, totalAmount, userCouponId);

        // when
        order.setStatus(Order.OrderStatus.COMPLETED);

        // then
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Order 주문 시간 설정")
    void setOrderOrderedAt() {
        // given
        Long userId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("20000");
        Long userCouponId = null;
        Order order = new Order(userId, orderItems, totalAmount, userCouponId);
        
        LocalDateTime orderedAt = LocalDateTime.now();

        // when
        order.setOrderedAt(orderedAt);

        // then
        assertThat(order.getOrderedAt()).isEqualTo(orderedAt);
    }

    @Test
    @DisplayName("Order 완료 처리")
    void completeOrder() {
        // given
        Long userId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("20000");
        Long userCouponId = null;
        Order order = new Order(userId, orderItems, totalAmount, userCouponId);

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
        Order order = new Order(userId, orderItems, totalAmount, userCouponId);

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
    @DisplayName("Order 기본 생성자")
    void createOrder_DefaultConstructor() {
        // when
        Order order = new Order();

        // then
        assertThat(order.getUserId()).isNull();
        assertThat(order.getOrderItems()).isEmpty();
        assertThat(order.getTotalAmount()).isNull();
        assertThat(order.getUserCouponId()).isNull();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Order 모든 필드 설정")
    void setOrderAllFields() {
        // given
        Long userId = 1L;
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = new BigDecimal("20000");
        Long userCouponId = 1L;
        BigDecimal discountedAmount = new BigDecimal("18000");
        Order.OrderStatus status = Order.OrderStatus.COMPLETED;
        LocalDateTime orderedAt = LocalDateTime.now();
        
        Order order = new Order();

        // when
        order.setUserId(userId);
        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setUserCouponId(userCouponId);
        order.setDiscountedAmount(discountedAmount);
        order.setStatus(status);
        order.setOrderedAt(orderedAt);

        // then
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getOrderItems()).isEqualTo(orderItems);
        assertThat(order.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(order.getUserCouponId()).isEqualTo(userCouponId);
        assertThat(order.getDiscountedAmount()).isEqualTo(discountedAmount);
        assertThat(order.getStatus()).isEqualTo(status);
        assertThat(order.getOrderedAt()).isEqualTo(orderedAt);
    }
} 