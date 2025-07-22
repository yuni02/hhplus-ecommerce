package kr.hhplus.be.server.order.application.port.out;

/**
 * 상품 재고 업데이트 Outgoing Port
 */
public interface UpdateProductStockPort {
    
    /**
     * 상품 재고 차감
     */
    boolean deductStock(Long productId, Integer quantity);
} 