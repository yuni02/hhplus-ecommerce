package kr.hhplus.be.server.product.domain.service;

import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.application.port.in.ProductRankingUseCase;
import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 인기 상품 조회 Application 서비스
 * Redis Sorted Set 기반 실시간 랭킹 사용
 */
@Service
public class GetPopularProductsService implements GetPopularProductsUseCase {

    private final LoadProductPort loadProductPort;
    private final ProductRankingUseCase productRankingService;

    public GetPopularProductsService(LoadProductPort loadProductPort,
                                   ProductRankingUseCase productRankingService) {
        this.loadProductPort = loadProductPort;
        this.productRankingService = productRankingService;
    }

    @Override
    // 임시로 캐시 비활성화 - Redis 직렬화 문제로 인해
    // @Cacheable(value = "popularProducts", key = "#command.limit", unless = "#result == null or #result.popularProducts.empty", cacheManager = "dailyAggregationCacheManager")
    public GetPopularProductsResult getPopularProducts(GetPopularProductsCommand command) {
        try {
            // 1. Redis에서 인기 상품 ID 목록 조회 (상위 N개)
            List<Long> topProductIds = productRankingService.getTopProductIds(command.getLimit());

            if (topProductIds.isEmpty()) {
                return new GetPopularProductsResult(List.of());
            }

            // 2. 상품 상세 정보 조회 및 결과 생성
            List<PopularProductInfo> popularProducts = topProductIds.stream()
                    .map(this::enrichProductInfoWithRanking)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            return new GetPopularProductsResult(popularProducts);

        } catch (Exception e) {
            return new GetPopularProductsResult(List.of());
        }
    }

    /**
     * 상품 정보에 랭킹 정보를 추가
     */
    private Optional<PopularProductInfo> enrichProductInfoWithRanking(Long productId) {
        try {
            // 상품 상세 정보 조회
            Optional<LoadProductPort.ProductInfo> productInfoOpt = loadProductPort.loadProductById(productId);
            
            if (productInfoOpt.isEmpty()) {
                return Optional.empty();
            }
            
            LoadProductPort.ProductInfo productInfo = productInfoOpt.get();
            
            // Redis에서 랭킹과 판매량 정보를 한 번에 조회 (통신 최적화)
            ProductRankingUseCase.ProductRankingInfo rankingInfo = productRankingService.getProductRankingInfo(productId);
            
            Long rank = rankingInfo.getRank() != null ? rankingInfo.getRank() : 0L;
            Integer recentSalesCount = rankingInfo.getSalesCount();

            return Optional.of(new PopularProductInfo(
                    productId,
                    productInfo.getName(),
                    productInfo.getCurrentPrice(),
                    productInfo.getStock(),
                    recentSalesCount, // totalSalesCount = recentSalesCount (Redis 기반)
                    0L, // totalSalesAmount - Redis에서는 수량만 관리
                    recentSalesCount,
                    0L, // recentSalesAmount - Redis에서는 수량만 관리
                    0.0, // conversionRate - 계산하지 않음
                    null, // lastOrderDate - 관리하지 않음
                    rank.intValue() + 1 // Redis rank는 0부터 시작하므로 +1
            ));
            
        } catch (Exception e) {
            return Optional.empty();
        }
    }
} 