package kr.hhplus.be.server.product.domain;

import java.util.List;
import java.util.Optional;

/**
 * 상품 서비스 인터페이스
 * 상품 관련 비즈니스 로직을 캡슐화
 */
public interface ProductService {
    
    /**
     * 상품 검증
     */
    ProductValidationResult validateProduct(Long productId, Integer quantity);
    
    /**
     * 재고 차감
     */
    StockDeductResult deductStock(Long productId, Integer quantity);
    
    /**
     * 상품 조회
     */
    Optional<Product> findById(Long productId);
    
    /**
     * 활성 상품 목록 조회
     */
    List<Product> findAllActive();
}

 