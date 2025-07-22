package kr.hhplus.be.server.product.adapter.in.dto;

public class ProductRequest {
    private String name;
    private Integer currentPrice;
    private Integer stock;

    public ProductRequest() {
    }

    public ProductRequest(String name, Integer currentPrice, Integer stock) {
        this.name = name;
        this.currentPrice = currentPrice;
        this.stock = stock;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Integer currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
} 