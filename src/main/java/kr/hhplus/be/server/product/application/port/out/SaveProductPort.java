package kr.hhplus.be.server.product.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 저장 Outgoing Port
 */
public interface SaveProductPort {
    
    /**
     * 상품 저장
     */
    ProductInfo saveProduct(ProductInfo productInfo);
    
    /**
     * 상품 정보
     */
    class ProductInfo {
        private final Long id;
        private final String name;
        private final String description;
        private final BigDecimal currentPrice;
        private final Integer stock;
        private final String status;
        private final String category;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
        
        public ProductInfo(Long id, String name, String description, BigDecimal currentPrice,
                         Integer stock, String status, String category, 
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.currentPrice = currentPrice;
            this.stock = stock;
            this.status = status;
            this.category = category;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public Integer getStock() { return stock; }
        public String getStatus() { return status; }
        public String getCategory() { return category; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        
        public static class Builder {
            private Long id;
            private String name;
            private String description;
            private BigDecimal currentPrice;
            private Integer stock;
            private String status;
            private String category;
            private LocalDateTime createdAt;
            private LocalDateTime updatedAt;
            
            public Builder id(Long id) { this.id = id; return this; }
            public Builder name(String name) { this.name = name; return this; }
            public Builder description(String description) { this.description = description; return this; }
            public Builder currentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; return this; }
            public Builder stock(Integer stock) { this.stock = stock; return this; }
            public Builder status(String status) { this.status = status; return this; }
            public Builder category(String category) { this.category = category; return this; }
            public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
            public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
            
            public ProductInfo build() {
                return new ProductInfo(id, name, description, currentPrice, stock, status, category, createdAt, updatedAt);
            }
        }
    }
}
