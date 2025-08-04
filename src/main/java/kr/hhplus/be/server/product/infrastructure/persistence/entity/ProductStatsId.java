package kr.hhplus.be.server.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * ProductStats 복합키 클래스
 * Product 도메인 전용 인프라스트럭처
 */
@Embeddable
public class ProductStatsId implements Serializable {
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "date")
    private LocalDate date;

    public ProductStatsId() {}

    public ProductStatsId(Long productId, LocalDate date) {
        this.productId = productId;
        this.date = date;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductStatsId that = (ProductStatsId) o;
        return Objects.equals(productId, that.productId) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, date);
    }
}