package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import kr.hhplus.be.server.product.application.port.out.LoadProductStatsPort;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 인기 상품 조회 Application 서비스
 */
@Service
public class GetPopularProductsService implements GetPopularProductsUseCase {

    private final LoadProductPort loadProductPort;
    private final LoadProductStatsPort loadProductStatsPort;

    public GetPopularProductsService(LoadProductPort loadProductPort,
                                   LoadProductStatsPort loadProductStatsPort) {
        this.loadProductPort = loadProductPort;
        this.loadProductStatsPort = loadProductStatsPort;
    }

    @Override
    public GetPopularProductsResult getPopularProducts(GetPopularProductsCommand command) {
        try {
            // 1. 인기 상품 통계 조회 (상위 5개)
            List<LoadProductStatsPort.ProductStatsInfo> popularStats = loadProductStatsPort.loadTopProductsBySales(5);

            // 2. 상품 상세 정보 조회 및 결과 생성
            List<PopularProductInfo> popularProducts = popularStats.stream()
                    .map(this::enrichProductStatsInfo)
                    .collect(Collectors.toList());

            return new GetPopularProductsResult(popularProducts);

        } catch (Exception e) {
            return new GetPopularProductsResult(List.of());
        }
    }

    /**
     * 상품 통계 정보에 상품 상세 정보를 추가
     */
    private PopularProductInfo enrichProductStatsInfo(LoadProductStatsPort.ProductStatsInfo statsInfo) {
        // 상품 상세 정보 조회
        Optional<LoadProductPort.ProductInfo> productInfoOpt = loadProductPort.loadProductById(statsInfo.getProductId());
        
        String productName = productInfoOpt.map(LoadProductPort.ProductInfo::getName).orElse("알 수 없는 상품");
        Integer currentPrice = productInfoOpt.map(LoadProductPort.ProductInfo::getCurrentPrice).orElse(0);
        Integer stock = productInfoOpt.map(LoadProductPort.ProductInfo::getStock).orElse(0);

        return new PopularProductInfo(
                statsInfo.getProductId(),
                productName,
                currentPrice,
                stock,
                statsInfo.getTotalSalesCount(),
                statsInfo.getTotalSalesAmount(),
                statsInfo.getRecentSalesCount(),
                statsInfo.getRecentSalesAmount(),
                statsInfo.getConversionRate(),
                null, // lastOrderDate - 추가 정보가 없는 경우
                statsInfo.getRank()
        );
    }
} 