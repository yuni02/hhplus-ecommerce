package kr.hhplus.be.server.product.application.port.out;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 통계 데이터 조회 Outgoing Port
 */
public interface LoadOrderStatsPort {
    
    /**
     * 최근 3일간 상품별 판매 통계 조회
     */
    List<ProductSalesStats> loadRecentProductSalesStats(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 상품별 판매 통계 정보
     */
    class ProductSalesStats {
        private final Long productId;
        private final String productName;
        private final Integer totalQuantity;
        private final java.math.BigDecimal totalAmount;
        private final LocalDateTime lastOrderDate;
        
        public ProductSalesStats(Long productId, String productName, Integer totalQuantity, 
                               java.math.BigDecimal totalAmount, LocalDateTime lastOrderDate) {
            this.productId = productId;
            this.productName = productName;
            this.totalQuantity = totalQuantity;
            this.totalAmount = totalAmount;
            this.lastOrderDate = lastOrderDate;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public String getProductName() {
            return productName;
        }
        
        public Integer getTotalQuantity() {
            return totalQuantity;
        }
        
        public java.math.BigDecimal getTotalAmount() {
            return totalAmount;
        }
        
        public LocalDateTime getLastOrderDate() {
            return lastOrderDate;
        }
    }
} 