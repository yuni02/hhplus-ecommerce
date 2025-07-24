package kr.hhplus.be.server.product.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 통계 도메인 엔티티
 * 순수한 비즈니스 로직만 포함
 */
public class ProductStats {

    private Long id;
    private Long productId;
    private String productName;
    private Integer recentSalesCount; // 최근 3일간 판매량
    private BigDecimal recentSalesAmount; // 최근 3일간 판매액
    private Integer totalSalesCount; // 전체 판매량
    private BigDecimal totalSalesAmount; // 전체 판매액
    private Integer rank; // 인기 순위
    private BigDecimal conversionRate; // 전환율
    private LocalDateTime lastOrderDate; // 마지막 주문일
    private LocalDateTime aggregationDate; // 집계일
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductStats() {}

    public ProductStats(Long productId, String productName) {
        this.productId = productId;
        this.productName = productName;
        this.recentSalesCount = 0;
        this.recentSalesAmount = BigDecimal.ZERO;
        this.totalSalesCount = 0;
        this.totalSalesAmount = BigDecimal.ZERO;
        this.conversionRate = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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

    public void addSale(Integer quantity, BigDecimal amount) {
        this.totalSalesCount += quantity;
        this.totalSalesAmount = this.totalSalesAmount.add(amount);
        this.recentSalesCount += quantity;
        this.recentSalesAmount = this.recentSalesAmount.add(amount);
        this.lastOrderDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void calculateConversionRate(Integer totalViews) {
        if (totalViews > 0) {
            this.conversionRate = BigDecimal.valueOf(this.totalSalesCount)
                    .divide(BigDecimal.valueOf(totalViews), 4, BigDecimal.ROUND_HALF_UP);
        }
        this.updatedAt = LocalDateTime.now();
    }
}