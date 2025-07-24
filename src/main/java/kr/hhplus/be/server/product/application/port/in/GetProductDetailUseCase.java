package kr.hhplus.be.server.product.application.port.in;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 상품 상세 조회 Incoming Port (Use Case)
 */
public interface GetProductDetailUseCase {
    
    /**
     * 상품 상세 조회 실행
     */
    Optional<GetProductDetailResult> getProductDetail(GetProductDetailCommand command);
    
    /**
     * 상품 상세 조회 명령
     */
    class GetProductDetailCommand {
        private final Long productId;
        
        public GetProductDetailCommand(Long productId) {
            this.productId = productId;
        }
        
        public Long getProductId() {
            return productId;
        }
    }
    
    /**
     * 상품 상세 조회 결과
     */
    class GetProductDetailResult {
        private final Long id;
        private final String name;
        private final Integer currentPrice;
        private final Integer stock;
        private final String status;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
        
        public GetProductDetailResult(Long id, String name, Integer currentPrice, Integer stock,
                                   String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.name = name;
            this.currentPrice = currentPrice;
            this.stock = stock;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
        
        public Long getId() {
            return id;
        }
        
        public String getName() {
            return name;
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
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
} 