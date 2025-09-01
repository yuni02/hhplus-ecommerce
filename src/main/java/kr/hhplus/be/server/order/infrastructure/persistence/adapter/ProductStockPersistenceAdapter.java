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
    @CacheEvict(value = "productDetail", key = "#productId", cacheManager = "shortTermCacheManager", condition = "${spring.profiles.active:dev} != 'test'")
    public boolean deductStock(Long productId, Integer quantity) {
        try {
            ProductEntity product = productJpaRepository.findByIdWithLock(productId)
                    .orElse(null);
            
            if (product == null) {
                return false;
            }
            
            if (!product.hasStock(quantity)) {
                return false;
            }
            
            product.decreaseStock(quantity);
            productJpaRepository.save(product);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId", cacheManager = "shortTermCacheManager", condition = "${spring.profiles.active:dev} != 'test'")
    public boolean restoreStock(Long productId, Integer quantity) {
        try {
            ProductEntity product = productJpaRepository.findByIdWithLock(productId)
                    .orElse(null);
            
            if (product == null) {
                return false;
            }
            
            product.increaseStock(quantity);
            productJpaRepository.save(product);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId", cacheManager = "shortTermCacheManager", condition = "#result == true and ${spring.profiles.active:dev} != 'test'")
    public boolean deductStockWithPessimisticLock(Long productId, Integer quantity) {
        // 현재 재고 확인 (로깅용) - 캐시 우회
        Integer stockBefore = productJpaRepository.findCurrentStock(productId);
        if (stockBefore == null) stockBefore = -1;
        
        // 원자적 재고 차감 - 조건부 업데이트로 동시성 보장
        int updatedRows = productJpaRepository.deductStockAtomic(productId, quantity);
        
        // 업데이트 후 재고 확인 (로깅용) - 캐시 우회
        Integer stockAfter = productJpaRepository.findCurrentStock(productId);
        if (stockAfter == null) stockAfter = -1;
        
        log.debug("[STOCK_DEBUG] productId={}, quantity={}, stockBefore={}, stockAfter={}, updatedRows={}, result={}, threadId={}",
            productId, quantity, stockBefore, stockAfter, updatedRows, 
            (updatedRows == 1 ? "SUCCESS" : "FAILED"), Thread.currentThread().getId());
        
        // 업데이트된 행이 1개면 성공, 0개면 재고 부족으로 실패
        return updatedRows == 1;
    }

    @Override
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId", cacheManager = "shortTermCacheManager", condition = "${spring.profiles.active:dev} != 'test'")
    public boolean restoreStockWithPessimisticLock(Long productId, Integer quantity) {
        try {
            // 비관적 락으로 상품 조회
            ProductEntity product = productJpaRepository.findByIdWithLock(productId)
                    .orElse(null);
            
            if (product == null) {
                return false;
            }
            
            product.increaseStock(quantity);
            productJpaRepository.save(product);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
}