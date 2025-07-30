package kr.hhplus.be.server.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    @Test
    @DisplayName("OrderItem 생성 성공")
    void createOrderItem_Success() {
        // given
        Long orderId = 1L;
        Long productId = 1L;
        String productName = "테스트 상품";
        Integer quantity = 2;
        BigDecimal unitPrice = new BigDecimal("10000");

        // when
        OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();

        // then
        assertThat(orderItem.getOrderId()).isEqualTo(orderId);
        assertThat(orderItem.getProductId()).isEqualTo(productId);
        assertThat(orderItem.getProductName()).isEqualTo(productName);
        assertThat(orderItem.getQuantity()).isEqualTo(quantity);
        assertThat(orderItem.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(orderItem.getTotalPrice()).isEqualTo(new BigDecimal("20000"));
    }

    @Test
    @DisplayName("OrderItem ID 설정")
    void setOrderItemId() {
        // given
        Long orderId = 1L;
        Long productId = 1L;
        String productName = "테스트 상품";
        Integer quantity = 2;
        BigDecimal unitPrice = new BigDecimal("10000");
        OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();
        
        Long itemId = 1L;

        // when
        orderItem.setId(itemId);

        // then
        assertThat(orderItem.getId()).isEqualTo(itemId);
    }

    @Test
    @DisplayName("OrderItem 기본 생성자")
    void createOrderItem_DefaultConstructor() {
        // when
        OrderItem orderItem = new OrderItem();

        // then
        assertThat(orderItem.getOrderId()).isNull();
        assertThat(orderItem.getProductId()).isNull();
        assertThat(orderItem.getProductName()).isNull();
        assertThat(orderItem.getQuantity()).isNull();
        assertThat(orderItem.getUnitPrice()).isNull();
    }

    @Test
    @DisplayName("OrderItem 모든 필드 설정")
    void setOrderItemAllFields() {
        // given
        Long orderId = 1L;
        Long productId = 1L;
        String productName = "테스트 상품";
        Integer quantity = 2;
        BigDecimal unitPrice = new BigDecimal("10000");
        
        OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();

        // when
        orderItem.setOrderId(orderId);
        orderItem.setProductId(productId);
        orderItem.setProductName(productName);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);

        // then
        assertThat(orderItem.getOrderId()).isEqualTo(orderId);
        assertThat(orderItem.getProductId()).isEqualTo(productId);
        assertThat(orderItem.getProductName()).isEqualTo(productName);
        assertThat(orderItem.getQuantity()).isEqualTo(quantity);
        assertThat(orderItem.getUnitPrice()).isEqualTo(unitPrice);
    }

    @Test
    @DisplayName("OrderItem 총 가격 계산")
    void calculateOrderItemTotalPrice() {
        // given
        Long orderId = 1L;
        Long productId = 1L;
        String productName = "테스트 상품";
        Integer quantity = 3;
        BigDecimal unitPrice = new BigDecimal("5000");

        // when
        OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();

        // then
        assertThat(orderItem.getTotalPrice()).isEqualTo(new BigDecimal("15000"));
    }

    @Test
    @DisplayName("OrderItem 수량 변경")
    void changeOrderItemQuantity() {
        // given
        Long orderId = 1L;
        Long productId = 1L;
        String productName = "테스트 상품";
        Integer quantity = 2;
        BigDecimal unitPrice = new BigDecimal("10000");
        OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();
        
        Integer newQuantity = 5;

        // when
        orderItem.setQuantity(newQuantity);

        // then
        assertThat(orderItem.getQuantity()).isEqualTo(newQuantity);
        assertThat(orderItem.getTotalPrice()).isEqualTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("OrderItem 단가 변경")
    void changeOrderItemUnitPrice() {
        // given
        Long orderId = 1L;
        Long productId = 1L;
        String productName = "테스트 상품";
        Integer quantity = 2;
        BigDecimal unitPrice = new BigDecimal("10000");
                            OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();
        
        BigDecimal newUnitPrice = new BigDecimal("8000");

        // when
        orderItem.setUnitPrice(newUnitPrice);

        // then
        assertThat(orderItem.getUnitPrice()).isEqualTo(newUnitPrice);
        assertThat(orderItem.getTotalPrice()).isEqualTo(new BigDecimal("16000"));
    }

    @Test
    @DisplayName("OrderItem 상품명 변경")
    void changeOrderItemProductName() {
        // given
        Long orderId = 1L;
        Long productId = 1L;
        String productName = "테스트 상품";
        Integer quantity = 2;
        BigDecimal unitPrice = new BigDecimal("10000");
        OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();
        
        String newProductName = "새로운 상품명";

        // when
        orderItem.setProductName(newProductName);

        // then
        assertThat(orderItem.getProductName()).isEqualTo(newProductName);
    }

    @Test
    @DisplayName("OrderItem 주문 ID 변경")
    void changeOrderItemOrderId() {
        // given
        Long orderId = 1L;
        Long productId = 1L;
        String productName = "테스트 상품";
        Integer quantity = 2;
        BigDecimal unitPrice = new BigDecimal("10000");
        OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();
        
        Long newOrderId = 2L;

        // when
        orderItem.setOrderId(newOrderId);

        // then
        assertThat(orderItem.getOrderId()).isEqualTo(newOrderId);
    }

    @Test
    @DisplayName("OrderItem 상품 ID 변경")
    void changeOrderItemProductId() {
        // given
        Long orderId = 1L;
        Long productId = 1L;
        String productName = "테스트 상품";
        Integer quantity = 2;
        BigDecimal unitPrice = new BigDecimal("10000");
        OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();
        
        Long newProductId = 2L;

        // when
        orderItem.setProductId(newProductId);

        // then
        assertThat(orderItem.getProductId()).isEqualTo(newProductId);
    }
} 