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
    @Query("SELECT ps FROM ProductStatsEntity ps WHERE ps.id.productId = :productId AND ps.id.date = :date")
    Optional<ProductStatsEntity> findByProductIdAndDate(@Param("productId") Long productId, @Param("date") LocalDate date);

    /**
     * 특정 날짜의 모든 상품 통계 조회
     */
    @Query("SELECT ps FROM ProductStatsEntity ps WHERE ps.id.date = :date")
    List<ProductStatsEntity> findByDate(@Param("date") LocalDate date);

    /**
     * 상품 ID로 모든 통계 조회
     */
    @Query("SELECT ps FROM ProductStatsEntity ps WHERE ps.id.productId = :productId")
    List<ProductStatsEntity> findByProductId(@Param("productId") Long productId);

    /**
     * 날짜 범위로 상품 통계 조회
     */
    @Query("SELECT ps FROM ProductStatsEntity ps WHERE ps.id.date BETWEEN :startDate AND :endDate")
    List<ProductStatsEntity> findByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 최근 판매량 기준으로 인기 상품 조회
     */
    @Query("SELECT ps FROM ProductStatsEntity ps WHERE ps.id.date = :date ORDER BY ps.totalQuantity DESC")
    List<ProductStatsEntity> findTopProductsByRecentSales(@Param("date") LocalDate date);

    /**
     * 전체 판매액 기준으로 인기 상품 조회
     */
    @Query("SELECT ps FROM ProductStatsEntity ps WHERE ps.id.date = :date ORDER BY ps.totalSales DESC")
    List<ProductStatsEntity> findTopProductsByTotalSales(@Param("date") LocalDate date);

    /**
     * 특정 날짜의 통계 삭제
     */
    @Query("DELETE FROM ProductStatsEntity ps WHERE ps.id.date = :date")
    void deleteByDate(@Param("date") LocalDate date);
}