package kr.hhplus.be.server.product.adapter.in.dto;    

import java.time.LocalDateTime;

/**
 * 인기상품 판매 통계 응답 DTO
 */
public class PopularProductStatsResponse {
    private Long productId;
    private String productName;
    private Integer currentPrice;
    private Integer stock;
    private Integer totalSalesCount; // 총 판매 수량
    private Long totalSalesAmount; // 총 매출액
    private Integer recentSalesCount; // 최근 3일 판매 수량
    private Long recentSalesAmount; // 최근 3일 매출액
    private Double conversionRate; // 전환율 (판매량/조회수 등)
    private LocalDateTime lastOrderDate; // 마지막 주문 날짜
    private Integer rank; // 인기 순위

    public PopularProductStatsResponse() {
    }

    public PopularProductStatsResponse(Long productId, String productName, Integer currentPrice, Integer stock,
            Integer totalSalesCount, Long totalSalesAmount, Integer recentSalesCount,
            Long recentSalesAmount, Double conversionRate, LocalDateTime lastOrderDate,
            Integer rank) {
        this.productId = productId;
        this.productName = productName;
        this.currentPrice = currentPrice;
        this.stock = stock;
        this.totalSalesCount = totalSalesCount;
        this.totalSalesAmount = totalSalesAmount;
        this.recentSalesCount = recentSalesCount;
        this.recentSalesAmount = recentSalesAmount;
        this.conversionRate = conversionRate;
        this.lastOrderDate = lastOrderDate;
        this.rank = rank;
    }

    // Getters and Setters
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

    public Integer getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Integer currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getTotalSalesCount() {
        return totalSalesCount;
    }

    public void setTotalSalesCount(Integer totalSalesCount) {
        this.totalSalesCount = totalSalesCount;
    }

    public Long getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(Long totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }

    public Integer getRecentSalesCount() {
        return recentSalesCount;
    }

    public void setRecentSalesCount(Integer recentSalesCount) {
        this.recentSalesCount = recentSalesCount;
    }

    public Long getRecentSalesAmount() {
        return recentSalesAmount;
    }

    public void setRecentSalesAmount(Long recentSalesAmount) {
        this.recentSalesAmount = recentSalesAmount;
    }

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public LocalDateTime getLastOrderDate() {
        return lastOrderDate;
    }

    public void setLastOrderDate(LocalDateTime lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
} 