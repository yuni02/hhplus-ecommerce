package kr.hhplus.be.server.product.infrastructure.persistence.repository;

import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Product 엔티티 JPA Repository
 * Product 도메인 전용 데이터 접근 계층
 */
@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * 활성 상태의 상품 조회
     */
    List<ProductEntity> findByStatus(String status);


    
    /**
     * 원자적 재고 차감 (조건부 업데이트)
     */
    @Modifying
    @Query("UPDATE ProductEntity p SET p.stockQuantity = p.stockQuantity - :quantity, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int deductStockAtomic(@Param("productId") Long productId, @Param("quantity") Integer quantity);
    
    /**
     * 현재 재고 조회 (캐시 우회)
     */
    @Query(value = "SELECT stock_quantity FROM products WHERE id = :productId", nativeQuery = true)
    Integer findCurrentStock(@Param("productId") Long productId);
}