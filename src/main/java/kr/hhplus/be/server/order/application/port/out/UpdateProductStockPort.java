package kr.hhplus.be.server.order.application.port.out;

/**
 * 상품 재고 업데이트 Outgoing Port
 */
public interface UpdateProductStockPort {
    
    /**
     * 상품 재고 차감
     */
    boolean deductStock(Long productId, Integer quantity);
    
    /**
     * 상품 재고 복구 (차감된 수량만큼 다시 증가)
     */
    boolean restoreStock(Long productId, Integer quantity);
} 