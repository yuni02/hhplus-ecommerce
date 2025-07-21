package kr.hhplus.be.server.product.domain;

import kr.hhplus.be.server.shared.domain.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductStats extends BaseEntity {

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

    public ProductStats() {}

    public ProductStats(Long productId, String productName) {
        this.productId = productId;
        this.productName = productName;
        this.recentSalesCount = 0;
        this.recentSalesAmount = BigDecimal.ZERO;
        this.totalSalesCount = 0;
        this.totalSalesAmount = BigDecimal.ZERO;
        this.rank = 0;
        this.conversionRate = BigDecimal.ZERO;
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

    public void addSale(Integer quantity, BigDecimal amount) {
        this.recentSalesCount += quantity;
        this.recentSalesAmount = this.recentSalesAmount.add(amount);
        this.totalSalesCount += quantity;
        this.totalSalesAmount = this.totalSalesAmount.add(amount);
        this.lastOrderDate = LocalDateTime.now();
    }

    public void calculateConversionRate(Integer totalViews) {
        if (totalViews > 0) {
            this.conversionRate = BigDecimal.valueOf(totalSalesCount)
                    .divide(BigDecimal.valueOf(totalViews), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }
}