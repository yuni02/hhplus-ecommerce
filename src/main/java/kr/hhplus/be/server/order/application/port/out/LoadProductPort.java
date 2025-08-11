package kr.hhplus.be.server.order.application.port.out;

import java.math.BigDecimal;
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
     * 상품 ID로 조회 (비관적 락 적용)
     */
    Optional<ProductInfo> loadProductByIdWithLock(Long productId);
    
    /**
     * 상품 정보
     */
    class ProductInfo {
        private final Long id;
        private final String name;
        private final String description;
        private final Integer stock;
        private final BigDecimal currentPrice;
        private final String status;
        
        public ProductInfo(Long id, String name, String description, Integer stock, 
                         BigDecimal currentPrice, String status) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.stock = stock;
            this.currentPrice = currentPrice;
            this.status = status;
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
        
        public Integer getStock() {
            return stock;
        }
        
        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }
        
        public String getStatus() {
            return status;
        }
    }
} 