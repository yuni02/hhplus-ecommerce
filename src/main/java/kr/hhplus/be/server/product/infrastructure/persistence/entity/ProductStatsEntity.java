package kr.hhplus.be.server.product.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ProductStats 인프라스트럭처 엔티티
 * Product 도메인 전용 JPA 매핑 엔티티
 * 외래키 제약조건 없이 느슨한 결합으로 설계
 */
@Entity
@Table(name = "product_stats")
@IdClass(ProductStatsId.class)
public class ProductStatsEntity {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "quantity_sold", nullable = false)
    private Integer recentSalesCount; // 최근 3일간 판매량

    @Column(name = "revenue", nullable = false)
    private BigDecimal recentSalesAmount; // 최근 3일간 판매액

    @Column(name = "total_sales_count")
    private Integer totalSalesCount; // 전체 판매량

    @Column(name = "total_sales_amount")
    private BigDecimal totalSalesAmount; // 전체 판매액

    @Column(name = "product_rank")
    private Integer rank; // 인기 순위

    @Column(name = "conversion_rate")
    private BigDecimal conversionRate; // 전환율

    @Column(name = "last_order_date")
    private LocalDateTime lastOrderDate; // 마지막 주문일

    @Column(name = "aggregation_date")
    private LocalDateTime aggregationDate; // 집계일

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 느슨한 관계 - 외래키 제약조건 없음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private ProductEntity product;

    public ProductStatsEntity() {}

    public ProductStatsEntity(Long productId, LocalDate date) {
        this.productId = productId;
        this.date = date;
        this.recentSalesCount = 0;
        this.recentSalesAmount = BigDecimal.ZERO;
        this.totalSalesCount = 0;
        this.totalSalesAmount = BigDecimal.ZERO;
        this.rank = 0;
        this.conversionRate = BigDecimal.ZERO;
        this.aggregationDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public Integer getRecentSalesCount() {
        return recentSalesCount;
    }

    public void setRecentSalesCount(Integer recentSalesCount) {
        this.recentSalesCount = recentSalesCount;
    }

    public BigDecimal getRecentSalesAmount() {
        return recentSalesAmount;
    }

    public void setRecentSalesAmount(BigDecimal recentSalesAmount) {
        this.recentSalesAmount = recentSalesAmount;
    }

    public Integer getTotalSalesCount() {
        return totalSalesCount;
    }

    public void setTotalSalesCount(Integer totalSalesCount) {
        this.totalSalesCount = totalSalesCount;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public BigDecimal getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
    }

    public LocalDateTime getLastOrderDate() {
        return lastOrderDate;
    }

    public void setLastOrderDate(LocalDateTime lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
    }

    public LocalDateTime getAggregationDate() {
        return aggregationDate;
    }

    public void setAggregationDate(LocalDateTime aggregationDate) {
        this.aggregationDate = aggregationDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }
}