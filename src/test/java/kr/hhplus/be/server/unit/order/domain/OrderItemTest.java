package kr.hhplus.be.server.unit.order.domain;

import kr.hhplus.be.server.order.domain.Order;
import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.product.domain.Product;
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
        BigDecimal totalPrice = new BigDecimal("20000");

        // when
        OrderItem orderItem = OrderItem.builder()
                .orderId(orderId)
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .build();

        // then
        assertThat(orderItem.getOrderId()).isEqualTo(orderId);
        assertThat(orderItem.getProductId()).isEqualTo(productId);
        assertThat(orderItem.getProductName()).isEqualTo(productName);
        assertThat(orderItem.getQuantity()).isEqualTo(quantity);
        assertThat(orderItem.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(orderItem.getTotalPrice()).isEqualTo(totalPrice);
    }

    @Test
    @DisplayName("OrderItem withOrder 메서드 테스트")
    void withOrder() {
        // given
        OrderItem orderItem = OrderItem.builder()
                .productId(1L)
                .productName("테스트 상품")
                .quantity(2)
                .unitPrice(new BigDecimal("10000"))
                .build();
        
        Order order = Order.builder()
                .id(100L)
                .userId(1L)
                .totalAmount(new BigDecimal("20000"))
                .build();

        // when
        OrderItem updatedItem = orderItem.withOrder(order);

        // then
        assertThat(updatedItem.getOrder()).isEqualTo(order);
        assertThat(updatedItem.getOrderId()).isEqualTo(100L);
        assertThat(updatedItem.getProductId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("OrderItem withProduct 메서드 테스트")
    void withProduct() {
        // given
        OrderItem orderItem = OrderItem.builder()
                .orderId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("10000"))
                .build();
        
        Product product = Product.builder()
                .id(200L)
                .name("새로운 상품")
                .build();

        // when
        OrderItem updatedItem = orderItem.withProduct(product);

        // then
        assertThat(updatedItem.getProduct()).isEqualTo(product);
        assertThat(updatedItem.getProductId()).isEqualTo(200L);
        assertThat(updatedItem.getProductName()).isEqualTo("새로운 상품");
    }

    @Test
    @DisplayName("OrderItem withOrder null 처리")
    void withOrder_NullHandling() {
        // given
        OrderItem orderItem = OrderItem.builder()
                .productId(1L)
                .productName("테스트 상품")
                .quantity(2)
                .unitPrice(new BigDecimal("10000"))
                .build();

        // when
        OrderItem result = orderItem.withOrder(null);

        // then
        assertThat(result).isEqualTo(orderItem);
        assertThat(result.getOrder()).isNull();
        assertThat(result.getOrderId()).isNull();
    }

    @Test
    @DisplayName("OrderItem withProduct null 처리")
    void withProduct_NullHandling() {
        // given
        OrderItem orderItem = OrderItem.builder()
                .orderId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("10000"))
                .build();

        // when
        OrderItem result = orderItem.withProduct(null);

        // then
        assertThat(result).isEqualTo(orderItem);
        assertThat(result.getProduct()).isNull();
        assertThat(result.getProductId()).isNull();
    }

    @Test
    @DisplayName("OrderItem toBuilder 테스트")
    void toBuilderTest() {
        // given
        OrderItem original = OrderItem.builder()
                .id(1L)
                .orderId(10L)
                .productId(20L)
                .productName("원본 상품")
                .quantity(2)
                .unitPrice(new BigDecimal("10000"))
                .totalPrice(new BigDecimal("20000"))
                .build();

        // when
        OrderItem modified = original.toBuilder()
                .quantity(3)
                .totalPrice(new BigDecimal("30000"))
                .build();

        // then
        assertThat(modified.getId()).isEqualTo(1L);
        assertThat(modified.getOrderId()).isEqualTo(10L);
        assertThat(modified.getProductId()).isEqualTo(20L);
        assertThat(modified.getProductName()).isEqualTo("원본 상품");
        assertThat(modified.getQuantity()).isEqualTo(3);
        assertThat(modified.getTotalPrice()).isEqualTo(new BigDecimal("30000"));
        
        // 원본 객체는 변경되지 않음
        assertThat(original.getQuantity()).isEqualTo(2);
        assertThat(original.getTotalPrice()).isEqualTo(new BigDecimal("20000"));
    }
}