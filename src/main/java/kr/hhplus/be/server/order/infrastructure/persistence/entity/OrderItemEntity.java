package kr.hhplus.be.server.order.infrastructure.persistence.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.shared.domain.BaseEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 주문 아이템 전용 엔티티
 * 주문 아이템 도메인 전용 JPA 매핑 엔티티
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private OrderEntity order;

    @Column(name = "order_id", nullable = false, insertable = false, updatable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ProductEntity product;

    @Column(name = "product_id", nullable = false, insertable = false, updatable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    // 비즈니스 메서드들
    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
        this.totalPrice = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public void updateUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        this.totalPrice = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }
}