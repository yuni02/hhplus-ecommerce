package kr.hhplus.be.server.order.domain;

/**
 * 주문 검증 결과
 */
public class OrderValidationResult {
    private final boolean valid;
    private final String errorMessage;
    
    public OrderValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public static OrderValidationResult success() {
        return new OrderValidationResult(true, null);
    }
    
    public static OrderValidationResult failure(String errorMessage) {
        return new OrderValidationResult(false, errorMessage);
    }
} 