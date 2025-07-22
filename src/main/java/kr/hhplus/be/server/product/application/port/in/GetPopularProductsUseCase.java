package kr.hhplus.be.server.product.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 인기 상품 조회 Incoming Port (Use Case)
 */
public interface GetPopularProductsUseCase {
    
    /**
     * 인기 상품 조회 실행
     */
    GetPopularProductsResult getPopularProducts(GetPopularProductsCommand command);
    
    /**
     * 인기 상품 조회 명령
     */
    class GetPopularProductsCommand {
        private final int limit;
        
        public GetPopularProductsCommand(int limit) {
            this.limit = limit;
        }
        
        public int getLimit() {
            return limit;
        }
    }
    
    /**
     * 인기 상품 조회 결과
     */
    class GetPopularProductsResult {
        private final List<PopularProductInfo> popularProducts;
        
        public GetPopularProductsResult(List<PopularProductInfo> popularProducts) {
            this.popularProducts = popularProducts;
        }
        
        public List<PopularProductInfo> getPopularProducts() {
            return popularProducts;
        }
    }
    
    /**
     * 인기 상품 정보
     */
    class PopularProductInfo {
        private final Long productId;
        private final String productName;
        private final Integer currentPrice;
        private final Integer stock;
        private final Integer totalSalesCount;
        private final Long totalSalesAmount;
        private final Integer recentSalesCount;
        private final Long recentSalesAmount;
        private final Double conversionRate;
        private final LocalDateTime lastOrderDate;
        private final Integer rank;
        
        public PopularProductInfo(Long productId, String productName, Integer currentPrice, Integer stock,
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
        
        public Long getProductId() {
            return productId;
        }
        
        public String getProductName() {
            return productName;
        }
        
        public Integer getCurrentPrice() {
            return currentPrice;
        }
        
        public Integer getStock() {
            return stock;
        }
        
        public Integer getTotalSalesCount() {
            return totalSalesCount;
        }
        
        public Long getTotalSalesAmount() {
            return totalSalesAmount;
        }
        
        public Integer getRecentSalesCount() {
            return recentSalesCount;
        }
        
        public Long getRecentSalesAmount() {
            return recentSalesAmount;
        }
        
        public Double getConversionRate() {
            return conversionRate;
        }
        
        public LocalDateTime getLastOrderDate() {
            return lastOrderDate;
        }
        
        public Integer getRank() {
            return rank;
        }
    }
} 