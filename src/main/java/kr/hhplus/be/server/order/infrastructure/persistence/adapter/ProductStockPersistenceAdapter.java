package kr.hhplus.be.server.order.infrastructure.persistence.adapter;

import kr.hhplus.be.server.order.application.port.out.UpdateProductStockPort;
import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import kr.hhplus.be.server.product.infrastructure.persistence.repository.ProductJpaRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product 재고 업데이트 영속성 Adapter (Order 도메인용)
 * Order 도메인에서 상품 재고 차감/복구를 위한 어댑터
 */
@Slf4j
@Component("orderProductStockPersistenceAdapter")
public class ProductStockPersistenceAdapter implements UpdateProductStockPort {

    private final ProductJpaRepository productJpaRepository;

    public ProductStockPersistenceAdapter(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId", cacheManager = "shortTermCacheManager", condition = "!@environment.acceptsProfiles('test')")
    public boolean deductStock(Long productId, Integer quantity) {
        try {
            // 원자적 재고 차감 쿼리 사용
            int updated = productJpaRepository.deductStockAtomic(productId, quantity);
            return updated > 0;
            
        } catch (Exception e) {
            log.warn("재고 차감 실패 - productId: {}, quantity: {}", productId, quantity, e);
            return false;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId", cacheManager = "shortTermCacheManager", condition = "!@environment.acceptsProfiles('test')")
    public boolean restoreStock(Long productId, Integer quantity) {
        try {
            // 원자적 재고 복구를 위한 쿼리 필요 - 일단 findById로 낙관적 락 사용
            ProductEntity product = productJpaRepository.findById(productId)
                    .orElse(null);
            
            if (product == null) {
                return false;
            }
            
            product.increaseStock(quantity);
            productJpaRepository.save(product); // @Version으로 낙관적 락 적용
            return true;
            
        } catch (Exception e) {
            log.warn("재고 복구 실패 - productId: {}, quantity: {}", productId, quantity, e);
            return false;
        }
    }

}