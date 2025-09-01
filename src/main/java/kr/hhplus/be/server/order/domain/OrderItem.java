package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.product.domain.Product;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주문 아이템 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Order order;
    private Product product;

    // 비즈니스 로직 메서드들
    // 비즈니스 로직에 필요한 경우 toBuilder()를 사용하여 불변 객체로 처리
    public OrderItem withOrder(Order order) {
        if (order == null) return this;
        return this.toBuilder()
                .order(order)
                .orderId(order.getId())
                .build();
    }

    public OrderItem withProduct(Product product) {
        if (product == null) return this;
        return this.toBuilder()
                .product(product)
                .productId(product.getId())
                .productName(product.getName())
                .build();
    }

    public void calculateTotalPrice() {
        if (quantity != null && unitPrice != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public BigDecimal getTotalPrice() {
        if (totalPrice == null && quantity != null && unitPrice != null) {
            calculateTotalPrice();
        }
        return totalPrice;
    }
} 