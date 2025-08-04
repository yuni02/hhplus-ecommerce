package kr.hhplus.be.server.product.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 통계 전용 엔티티
 * 상품 통계 도메인 전용 JPA 매핑 엔티티
 */
@Entity
@Table(name = "product_stats")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter(AccessLevel.PRIVATE) // setter는 private으로 제한
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatsEntity {

    @EmbeddedId
    private ProductStatsId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id", referencedColumnName = "id",
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ProductEntity product;

    @Column(name = "total_sales", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private Integer totalQuantity = 0;

    @Column(name = "order_count", nullable = false)
    @Builder.Default
    private Integer orderCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 비즈니스 메서드들
    public void updateStats(BigDecimal sales, Integer quantity, Integer orderCount) {
        this.totalSales = sales;
        this.totalQuantity = quantity;
        this.orderCount = orderCount;
    }

    public void incrementStats(BigDecimal sales, Integer quantity) {
        this.totalSales = this.totalSales.add(sales);
        this.totalQuantity += quantity;
        this.orderCount++;
    }
}