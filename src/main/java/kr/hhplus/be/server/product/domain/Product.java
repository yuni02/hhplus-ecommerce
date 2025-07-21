package kr.hhplus.be.server.product.domain;

import kr.hhplus.be.server.shared.domain.BaseEntity;

import java.math.BigDecimal;

public class Product extends BaseEntity {

    private String name;
    private String description;
    private BigDecimal currentPrice;
    private Integer stock;
    private ProductStatus status = ProductStatus.ACTIVE;
    private String category;

    public Product() {}

    public Product(String name, String description, BigDecimal currentPrice, Integer stock, String category) {
        this.name = name;
        this.description = description;
        this.currentPrice = currentPrice;
        this.stock = stock;
        this.category = category;
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

    public boolean isActive() {
        return status == ProductStatus.ACTIVE;
    }

    public boolean hasStock(Integer quantity) {
        return stock >= quantity;
    }

    public void decreaseStock(Integer quantity) {
        if (!hasStock(quantity)) {
            throw new InsufficientStockException("재고가 부족합니다.");
        }
        this.stock -= quantity;
    }

    public void increaseStock(Integer quantity) {
        this.stock += quantity;
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