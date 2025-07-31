package kr.hhplus.be.server.product.application.port.out;

import kr.hhplus.be.server.product.domain.ProductStats;
import java.time.LocalDate;
import java.util.List;

/**
 * 상품 통계 저장 Outgoing Port
 */
public interface SaveProductStatsPort {
    
    /**
     * 상품 통계 저장
     */
    ProductStats saveProductStats(ProductStats productStats);
    
    /**
     * 상품 통계 목록 저장
     */
    List<ProductStats> saveAllProductStats(List<ProductStats> productStatsList);
    
    /**
     * 특정 날짜의 기존 통계 삭제
     */
    void deleteProductStatsByDate(LocalDate date);
} 