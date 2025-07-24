package kr.hhplus.be.server.product.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * 상품 조회 Outgoing Port
 */
public interface LoadProductPort {
    
    /**
     * 상품 ID로 조회
     */
    Optional<ProductInfo> loadProductById(Long productId);
    
    /**
     * 활성 상품 목록 조회
     */
    List<ProductInfo> loadAllActiveProducts();
    
    /**
     * 상품 정보
     */
    class ProductInfo {
        private final Long id;
        private final String name;
        private final String description;
        private final Integer currentPrice;
        private final Integer stock;
        private final String status;
        private final String category;
        
        public ProductInfo(Long id, String name, String description, Integer currentPrice,
                          Integer stock, String status, String category) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.currentPrice = currentPrice;
            this.stock = stock;
            this.status = status;
            this.category = category;
        }
        
        public Long getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Integer getCurrentPrice() {
            return currentPrice;
        }
        
        public Integer getStock() {
            return stock;
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getCategory() {
            return category;
        }
    }
} 