package kr.hhplus.be.server.product.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 */
public class Product {

    private Long id;
    private String name;
    private String description;
    private BigDecimal currentPrice;
    private Integer stock;
    private ProductStatus status = ProductStatus.ACTIVE;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(String name, String description, BigDecimal currentPrice, Integer stock, String category) {
        this.name = name;
        this.description = description;
        this.currentPrice = currentPrice;
        this.stock = stock;
        this.category = category;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return status == ProductStatus.ACTIVE;
    }

    public boolean hasStock(Integer quantity) {
        return stock >= quantity;
    }

    public void decreaseStock(Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감할 수량은 양수여야 합니다.");
        }
        if (stock < quantity) {
            throw new InsufficientStockException("재고가 부족합니다. 현재 재고: " + stock);
        }
        this.stock -= quantity;
        this.updatedAt = LocalDateTime.now();
        
        // 재고가 0이 되면 상태를 SOLD_OUT으로 변경
        if (this.stock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        }
    }

    public void increaseStock(Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("증가할 수량은 양수여야 합니다.");
        }
        this.stock += quantity;
        this.updatedAt = LocalDateTime.now();
        
        // 재고가 생기면 상태를 ACTIVE로 변경 (SOLD_OUT에서만)
        if (this.status == ProductStatus.SOLD_OUT && this.stock > 0) {
            this.status = ProductStatus.ACTIVE;
        }
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