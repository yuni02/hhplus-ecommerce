package kr.hhplus.be.server.product.adapter.out.persistence;

import kr.hhplus.be.server.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Product 엔티티 JPA Repository
 */
@Repository
public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    /**
     * 활성 상태인 상품 목록 조회
     */
    List<Product> findByStatus(Product.ProductStatus status);

    /**
     * 활성 상태이며 재고가 있는 상품 조회
     */
    List<Product> findByStatusAndStockGreaterThan(Product.ProductStatus status, Integer stock);

    /**
     * 특정 상품의 재고 차감 (동시성 제어)
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :productId AND p.stock >= :quantity")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 특정 상품의 재고 증가
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :productId")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 활성 상품 조회 (락 사용)
     */
    Optional<Product> findByIdAndStatus(Long productId, Product.ProductStatus status);

    /**
     * 인기 상품 조회 (판매량 기준)
     */
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN ProductStats ps ON p.id = ps.productId " +
           "WHERE p.status = 'ACTIVE' AND ps.date BETWEEN :startDate AND :endDate " +
           "ORDER BY ps.recentSalesCount DESC")
    List<Product> findPopularProductsBySalesCount(@Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate, 
                                                 Pageable pageable);

    /**
     * 카테고리별 인기 상품 조회
     */
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN ProductStats ps ON p.id = ps.productId " +
           "WHERE p.status = 'ACTIVE' AND p.category = :category AND ps.date BETWEEN :startDate AND :endDate " +
           "ORDER BY ps.recentSalesCount DESC")
    List<Product> findPopularProductsByCategory(@Param("category") String category,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               Pageable pageable);

    /**
     * 가격대별 상품 조회
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.status = 'ACTIVE' AND p.currentPrice BETWEEN :minPrice AND :maxPrice " +
           "ORDER BY p.currentPrice ASC")
    Page<Product> findProductsByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                          @Param("maxPrice") BigDecimal maxPrice,
                                          Pageable pageable);

    /**
     * 검색어로 상품 조회 (이름, 설명)
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.status = 'ACTIVE' AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.name ASC")
    Page<Product> searchProductsByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 재고 부족 상품 조회
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.status = 'ACTIVE' AND p.stock <= :threshold " +
           "ORDER BY p.stock ASC")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);
} 