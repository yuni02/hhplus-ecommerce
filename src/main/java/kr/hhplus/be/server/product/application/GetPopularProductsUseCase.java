package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.ProductDomainService;
import kr.hhplus.be.server.product.domain.ProductStats;
import kr.hhplus.be.server.product.domain.ProductStatsRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 인기 상품 조회 UseCase
 * 여러 도메인 서비스를 조합하여 사용
 */
@Component
public class GetPopularProductsUseCase {

    private final ProductStatsRepository productStatsRepository;

    public GetPopularProductsUseCase(ProductStatsRepository productStatsRepository) {
        this.productStatsRepository = productStatsRepository;
    }

    public Output execute(Input input) {
        // 도메인 서비스를 통한 인기 상품 순위 계산
        List<ProductStats> allStats = productStatsRepository.findAll();
        List<ProductStats> popularStats = ProductDomainService.calculatePopularityRanking(allStats, input.limit);
        
        List<PopularProductOutput> popularProducts = popularStats.stream()
                .map(stats -> new PopularProductOutput(
                    stats.getProductId(),
                    stats.getProductName(),
                    0, // currentPrice는 별도로 조회 필요
                    0, // stock은 별도로 조회 필요
                    stats.getTotalSalesCount(),
                    stats.getTotalSalesAmount().longValue(),
                    stats.getRecentSalesCount(),
                    stats.getRecentSalesAmount().longValue(),
                    stats.getConversionRate().doubleValue(),
                    stats.getLastOrderDate(),
                    stats.getRank()
                ))
                .collect(Collectors.toList());
        
        return new Output(popularProducts);
    }

    public static class Input {
        private final int limit;

        public Input(int limit) {
            this.limit = limit;
        }

        public int getLimit() {
            return limit;
        }
    }

    public static class Output {
        private final List<PopularProductOutput> popularProducts;

        public Output(List<PopularProductOutput> popularProducts) {
            this.popularProducts = popularProducts;
        }

        public List<PopularProductOutput> getPopularProducts() {
            return popularProducts;
        }
    }

    public static class PopularProductOutput {
        private final Long productId;
        private final String productName;
        private final Integer currentPrice;
        private final Integer stock;
        private final Integer totalSalesCount;
        private final Long totalSalesAmount;
        private final Integer recentSalesCount;
        private final Long recentSalesAmount;
        private final Double conversionRate;
        private final LocalDateTime lastOrderDate;
        private final Integer rank;

        public PopularProductOutput(Long productId, String productName, Integer currentPrice, Integer stock,
                                  Integer totalSalesCount, Long totalSalesAmount, Integer recentSalesCount,
                                  Long recentSalesAmount, Double conversionRate, LocalDateTime lastOrderDate,
                                  Integer rank) {
            this.productId = productId;
            this.productName = productName;
            this.currentPrice = currentPrice;
            this.stock = stock;
            this.totalSalesCount = totalSalesCount;
            this.totalSalesAmount = totalSalesAmount;
            this.recentSalesCount = recentSalesCount;
            this.recentSalesAmount = recentSalesAmount;
            this.conversionRate = conversionRate;
            this.lastOrderDate = lastOrderDate;
            this.rank = rank;
        }

        public Long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public Integer getCurrentPrice() {
            return currentPrice;
        }

        public Integer getStock() {
            return stock;
        }

        public Integer getTotalSalesCount() {
            return totalSalesCount;
        }

        public Long getTotalSalesAmount() {
            return totalSalesAmount;
        }

        public Integer getRecentSalesCount() {
            return recentSalesCount;
        }

        public Long getRecentSalesAmount() {
            return recentSalesAmount;
        }

        public Double getConversionRate() {
            return conversionRate;
        }

        public LocalDateTime getLastOrderDate() {
            return lastOrderDate;
        }

        public Integer getRank() {
            return rank;
        }
    }
}