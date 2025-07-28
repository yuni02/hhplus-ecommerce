package kr.hhplus.be.server.product.adapter.out.persistence;

import kr.hhplus.be.server.product.domain.ProductStats;
import kr.hhplus.be.server.product.domain.ProductStatsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ProductStats 엔티티 JPA Repository
 */
@Repository
public interface ProductStatsJpaRepository extends JpaRepository<ProductStats, ProductStatsId> {

    /**
     * 특정 상품의 통계 조회
     */
    List<ProductStats> findByProductId(Long productId);

    /**
     * 특정 날짜의 통계 조회
     */
    List<ProductStats> findByDate(LocalDate date);

    /**
     * 특정 상품의 최신 통계 조회
     */
    @Query("SELECT ps FROM ProductStats ps WHERE ps.productId = :productId ORDER BY ps.date DESC")
    List<ProductStats> findLatestByProductId(@Param("productId") Long productId);

    /**
     * 판매량 기준 상위 상품 조회
     */
    @Query("SELECT ps FROM ProductStats ps WHERE ps.date = :date ORDER BY ps.recentSalesCount DESC")
    List<ProductStats> findTopProductsBySalesOnDate(@Param("date") LocalDate date);

    /**
     * 최근 N일간의 인기 상품 조회
     */
    @Query("SELECT ps FROM ProductStats ps WHERE ps.date >= :startDate ORDER BY ps.recentSalesCount DESC")
    List<ProductStats> findTopProductsByRecentSales(@Param("startDate") LocalDate startDate);

    /**
     * 전체 상품의 최신 통계 조회
     */
    @Query("SELECT ps FROM ProductStats ps WHERE ps.date = (SELECT MAX(ps2.date) FROM ProductStats ps2 WHERE ps2.productId = ps.productId)")
    List<ProductStats> findLatestStatsForAllProducts();

    /**
     * 특정 상품의 특정 날짜 통계 조회
     */
    Optional<ProductStats> findByProductIdAndDate(Long productId, LocalDate date);
} 