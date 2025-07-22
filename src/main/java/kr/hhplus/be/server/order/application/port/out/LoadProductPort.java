package kr.hhplus.be.server.order.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 상품 조회 Outgoing Port
 */
public interface LoadProductPort {
    
    /**
     * 상품 조회
     */
    Optional<ProductInfo> loadProductById(Long productId);
    
    /**
     * 상품 정보
     */
    class ProductInfo {
        private final Long id;
        private final String name;
        private final BigDecimal currentPrice;
        private final Integer stock;
        
        public ProductInfo(Long id, String name, BigDecimal currentPrice, Integer stock) {
            this.id = id;
            this.name = name;
            this.currentPrice = currentPrice;
            this.stock = stock;
        }
        
        public Long getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }
        
        public Integer getStock() {
            return stock;
        }
    }
} 