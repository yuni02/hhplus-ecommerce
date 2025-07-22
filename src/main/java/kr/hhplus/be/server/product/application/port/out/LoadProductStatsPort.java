package kr.hhplus.be.server.product.application.port.out;

import java.util.List;

/**
 * 상품 통계 조회 Outgoing Port
 */
public interface LoadProductStatsPort {
    
    /**
     * 모든 상품 통계 조회
     */
    List<ProductStatsInfo> loadAllProductStats();
    
    /**
     * 상품 통계 정보
     */
    class ProductStatsInfo {
        private final Long productId;
        private final String productName;
        private final Integer recentSalesCount;
        private final Long recentSalesAmount;
        private final Integer totalSalesCount;
        private final Long totalSalesAmount;
        private final Integer rank;
        private final Double conversionRate;
        
        public ProductStatsInfo(Long productId, String productName, Integer recentSalesCount,
                              Long recentSalesAmount, Integer totalSalesCount, Long totalSalesAmount,
                              Integer rank, Double conversionRate) {
            this.productId = productId;
            this.productName = productName;
            this.recentSalesCount = recentSalesCount;
            this.recentSalesAmount = recentSalesAmount;
            this.totalSalesCount = totalSalesCount;
            this.totalSalesAmount = totalSalesAmount;
            this.rank = rank;
            this.conversionRate = conversionRate;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public String getProductName() {
            return productName;
        }
        
        public Integer getRecentSalesCount() {
            return recentSalesCount;
        }
        
        public Long getRecentSalesAmount() {
            return recentSalesAmount;
        }
        
        public Integer getTotalSalesCount() {
            return totalSalesCount;
        }
        
        public Long getTotalSalesAmount() {
            return totalSalesAmount;
        }
        
        public Integer getRank() {
            return rank;
        }
        
        public Double getConversionRate() {
            return conversionRate;
        }
    }
} 