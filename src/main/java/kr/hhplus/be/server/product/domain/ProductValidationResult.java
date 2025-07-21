package kr.hhplus.be.server.product.domain;

/**
 * 상품 검증 결과
 */
public class ProductValidationResult {
    private final boolean valid;
    private final Product product;
    private final String errorMessage;
    
    public ProductValidationResult(boolean valid, Product product, String errorMessage) {
        this.valid = valid;
        this.product = product;
        this.errorMessage = errorMessage;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public static ProductValidationResult success(Product product) {
        return new ProductValidationResult(true, product, null);
    }
    
    public static ProductValidationResult failure(String errorMessage) {
        return new ProductValidationResult(false, null, errorMessage);
    }
} 