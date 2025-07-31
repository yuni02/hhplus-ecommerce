package kr.hhplus.be.server.order.infrastructure.persistence.adapter;

import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product 재고 업데이트 영속성 Adapter (Order 도메인용)
 * Order 도메인에서 상품 재고 차감/복구를 위한 어댑터
 */
@Component("orderProductStockPersistenceAdapter")
public class ProductStockPersistenceAdapter implements UpdateProductStockPort {

    private final ProductJpaRepository productJpaRepository;

    public ProductStockPersistenceAdapter(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    @Transactional
    public boolean deductStock(Long productId, Integer quantity) {
        try {
            ProductEntity product = productJpaRepository.findById(productId)
                    .orElse(null);
            
            if (product == null) {
                return false;
            }
            
            boolean success = product.deductStock(quantity);
            if (success) {
                productJpaRepository.save(product);
            }
            return success;
            
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean restoreStock(Long productId, Integer quantity) {
        try {
            ProductEntity product = productJpaRepository.findById(productId)
                    .orElse(null);
            
            if (product == null) {
                return false;
            }
            
            product.restoreStock(quantity);
            productJpaRepository.save(product);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
}