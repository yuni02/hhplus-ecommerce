package kr.hhplus.be.server.product.application.port.in;

import java.util.List;

/**
 * 상품 랭킹 조회 Use Case
 * Redis 기반 실시간 랭킹과 DB 기반 통계를 모두 지원
 */
public interface ProductRankingUseCase {
    
    /**
     * 인기 상품 TOP N 조회
     */
    List<Long> getTopProductIds(int limit);
    
    /**
     * 특정 상품의 랭킹 조회
     */
    Long getProductRank(Long productId);
    
    /**
     * 특정 상품의 판매량 점수 조회
     */
    Double getProductSalesScore(Long productId);
    
    /**
     * 상품 랭킹 업데이트
     */
    void updateProductRanking(Long productId, Integer quantity);
}
