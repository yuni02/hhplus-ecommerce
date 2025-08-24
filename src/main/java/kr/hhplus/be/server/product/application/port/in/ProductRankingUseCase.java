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
    
    /**
     * 상품의 랭킹과 점수를 한 번에 조회
     * Redis 통신 횟수 최적화를 위한 메서드
     */
    ProductRankingInfo getProductRankingInfo(Long productId);
    
    /**
     * 상품 랭킹 정보
     */
    class ProductRankingInfo {
        private final Long rank;
        private final Double score;
        
        public ProductRankingInfo(Long rank, Double score) {
            this.rank = rank;
            this.score = score;
        }
        
        public Long getRank() { return rank; }
        public Double getScore() { return score; }
        public Integer getSalesCount() { return score != null ? score.intValue() : 0; }
    }
}
