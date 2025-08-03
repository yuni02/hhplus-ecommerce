package kr.hhplus.be.server.product.infrastructure.persistence.repository;

import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Product 엔티티 JPA Repository
 * Product 도메인 전용 데이터 접근 계층
 */
@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * 상품명으로 조회
     */
    List<ProductEntity> findByNameContaining(String name);

    /**
     * 활성 상태의 상품 조회
     */
    List<ProductEntity> findByStatus(String status);

    /**
     * 재고가 있는 상품 조회
     */
    List<ProductEntity> findByStockQuantityGreaterThan(Integer stockQuantity);

    /**
     * 가격 범위로 상품 조회
     */
    List<ProductEntity> findByPriceBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);
}