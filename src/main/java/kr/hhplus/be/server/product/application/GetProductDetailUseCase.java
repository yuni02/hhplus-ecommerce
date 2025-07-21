package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.ProductService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 상품 상세 조회 UseCase
 * 외부 의존성 없이 도메인 서비스만 호출
 */
@Component
public class GetProductDetailUseCase {

    private final ProductService productService;

    public GetProductDetailUseCase(ProductService productService) {
        this.productService = productService;
    }

    public Optional<Output> execute(Input input) {
        return productService.findById(input.productId)
                .map(product -> new Output(
                    product.getId(),
                    product.getName(),
                    product.getCurrentPrice().intValue(),
                    product.getStock(),
                    product.getStatus().name(),
                    product.getCreatedAt(),
                    product.getUpdatedAt()
                ));
    }

    public static class Input {
        private final Long productId;

        public Input(Long productId) {
            this.productId = productId;
        }

        public Long getProductId() {
            return productId;
        }
    }

    public static class Output {
        private final Long id;
        private final String name;
        private final Integer currentPrice;
        private final Integer stock;
        private final String status;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;

        public Output(Long id, String name, Integer currentPrice, Integer stock,
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