package kr.hhplus.be.server.order.domain;

import kr.hhplus.be.server.product.domain.Product;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 주문 아이템 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    // Note: 현재 사용되지 않는 메서드 (addOrderItem에서만 사용되었으나 해당 메서드 제거됨)
    public void setOrder(Order order) {
        this.order = order;
        if (order != null) {
            this.orderId = order.getId();
        }
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
            this.productName = product.getName();
        }
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