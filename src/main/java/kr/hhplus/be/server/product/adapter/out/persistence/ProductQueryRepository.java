package kr.hhplus.be.server.product.adapter.out.persistence;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 상품 복잡 쿼리용 QueryDSL Repository
 */
public interface ProductQueryRepository {

    /**
     * 인기 상품 조회 (판매량 기준)
     */
    List<Product> findPopularProductsBySalesCount(LocalDate startDate, LocalDate endDate, int limit);

    /**
     * 카테고리별 인기 상품 조회
     */
    List<Product> findPopularProductsByCategory(String category, LocalDate startDate, LocalDate endDate, int limit);

    /**
     * 가격대별 상품 조회
     */
    Page<Product> findProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * 검색어로 상품 조회 (이름, 설명)
     */
    Page<Product> searchProductsByKeyword(String keyword, Pageable pageable);

    /**
     * 재고 부족 상품 조회
     */
    List<Product> findLowStockProducts(int threshold);

    /**
     * 상품 통계와 함께 조회
     */
    List<ProductStats> findProductStatsWithProduct(LocalDate date);

    /**
     * 복합 조건으로 상품 검색
     */
    Page<Product> findProductsByComplexCondition(String category, BigDecimal minPrice, BigDecimal maxPrice, 
                                                String keyword, Pageable pageable);
} 