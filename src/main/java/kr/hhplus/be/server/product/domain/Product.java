package kr.hhplus.be.server.product.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Product {

    private Long id;
    private String name;
    private String description;
    private BigDecimal currentPrice;
    private Integer stock;
    
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;
    
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 비즈니스 로직 메서드들
    public boolean isAvailable() {
        return status == ProductStatus.ACTIVE && stock > 0;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
        if (stock < quantity) {
            throw new InsufficientStockException("재고가 부족합니다. 현재 재고: " + stock + ", 요청 수량: " + quantity);
        }
        this.stock -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
        }
        this.stock += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public enum ProductStatus {
        ACTIVE, INACTIVE, SOLD_OUT
    }

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }
}