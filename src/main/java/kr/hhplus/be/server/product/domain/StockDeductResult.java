package kr.hhplus.be.server.product.domain;

/**
 * 재고 차감 결과
 */
public class StockDeductResult {
    private final boolean success;
    private final Product product;
    private final String errorMessage;
    
    public StockDeductResult(boolean success, Product product, String errorMessage) {
        this.success = success;
        this.product = product;
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public static StockDeductResult success(Product product) {
        return new StockDeductResult(true, product, null);
    }
    
    public static StockDeductResult failure(String errorMessage) {
        return new StockDeductResult(false, null, errorMessage);
    }
} 