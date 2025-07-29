package kr.hhplus.be.server.product.infrastructure.persistence.repository;

import kr.hhplus.be.server.product.infrastructure.persistence.entity.ProductStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ProductStats 엔티티 JPA Repository
 * Product 도메인 전용 데이터 접근 계층
 */
@Repository
public interface ProductStatsJpaRepository extends JpaRepository<ProductStatsEntity, Long> {

    /**
     * 상품 ID와 날짜로 통계 조회
     */
    Optional<ProductStatsEntity> findByProductIdAndDate(Long productId, LocalDate date);

    /**
     * 특정 날짜의 모든 상품 통계 조회
     */
    List<ProductStatsEntity> findByDate(LocalDate date);

    /**
     * 상품 ID로 모든 통계 조회
     */
    List<ProductStatsEntity> findByProductId(Long productId);

    /**
     * 날짜 범위로 상품 통계 조회
     */
    List<ProductStatsEntity> findByDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 최근 판매량 기준으로 인기 상품 조회
     */
    @Query("SELECT ps FROM ProductStatsEntity ps WHERE ps.date = :date ORDER BY ps.recentSalesCount DESC")
    List<ProductStatsEntity> findTopProductsByRecentSales(@Param("date") LocalDate date);

    /**
     * 전체 판매액 기준으로 인기 상품 조회
     */
    @Query("SELECT ps FROM ProductStatsEntity ps WHERE ps.date = :date ORDER BY ps.totalSalesAmount DESC")
    List<ProductStatsEntity> findTopProductsByTotalSales(@Param("date") LocalDate date);
}