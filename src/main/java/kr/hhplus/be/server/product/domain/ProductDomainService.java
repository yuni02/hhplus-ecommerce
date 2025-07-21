package kr.hhplus.be.server.product.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 순수한 상품 도메인 로직만 포함하는 도메인 서비스
 * 프레임워크나 외부 의존성 없음
 */
public class ProductDomainService {

    /**
     * 상품 활성 상태 확인 도메인 로직
     */
    public static boolean isActiveProduct(Product product) {
        return product.isActive();
    }

    /**
     * 재고 확인 도메인 로직
     */
    public static boolean hasSufficientStock(Product product, Integer quantity) {
        return product.hasStock(quantity);
    }

    /**
     * 재고 차감 도메인 로직
     */
    public static Product decreaseStock(Product product, Integer quantity) {
        product.decreaseStock(quantity);
        return product;
    }

    /**
     * 재고 증가 도메인 로직
     */
    public static Product increaseStock(Product product, Integer quantity) {
        product.increaseStock(quantity);
        return product;
    }

    /**
     * 인기 상품 순위 계산 도메인 로직
     */
    public static List<ProductStats> calculatePopularityRanking(List<ProductStats> productStats, int limit) {
        List<ProductStats> sortedStats = productStats.stream()
                .sorted(Comparator.comparing(ProductStats::getRecentSalesCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        
        // 순위 설정
        for (int i = 0; i < sortedStats.size(); i++) {
            sortedStats.get(i).setRank(i + 1);
        }
        
        return sortedStats;
    }

    /**
     * 판매 통계 업데이트 도메인 로직
     */
    public static ProductStats updateSalesStats(ProductStats stats, Integer quantity, BigDecimal amount) {
        stats.addSale(quantity, amount);
        stats.setAggregationDate(LocalDateTime.now());
        return stats;
    }

    /**
     * 전환율 계산 도메인 로직
     */
    public static ProductStats calculateConversionRate(ProductStats stats, Integer totalViews) {
        stats.calculateConversionRate(totalViews);
        return stats;
    }

    /**
     * 상품 가격 계산 도메인 로직
     */
    public static BigDecimal calculateTotalPrice(Product product, Integer quantity) {
        return product.getCurrentPrice().multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 상품 유효성 검증 도메인 로직
     */
    public static boolean isValidProduct(Product product) {
        return product != null && 
               product.isActive() && 
               product.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0;
    }
}