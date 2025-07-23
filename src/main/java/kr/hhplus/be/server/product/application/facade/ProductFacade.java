package kr.hhplus.be.server.product.application.facade;

import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import kr.hhplus.be.server.product.application.port.out.LoadProductStatsPort;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 상품 관리 Facade
 * 복잡한 상품 관련 로직을 단순화하여 제공
 */
@Service
public class ProductFacade {

    private final LoadProductPort loadProductPort;
    private final LoadProductStatsPort loadProductStatsPort;

    public ProductFacade(LoadProductPort loadProductPort, LoadProductStatsPort loadProductStatsPort) {
        this.loadProductPort = loadProductPort;
        this.loadProductStatsPort = loadProductStatsPort;
    }

    /**
     * 상품 상세 조회 (Facade 메서드)
     */
    public Optional<GetProductDetailUseCase.GetProductDetailResult> getProductDetail(GetProductDetailUseCase.GetProductDetailCommand command) {
        try {
            // 1. 상품 조회
            Optional<LoadProductPort.ProductInfo> productInfoOpt = loadProductPort.loadProductById(command.getProductId());
            
            if (productInfoOpt.isEmpty()) {
                return Optional.empty();
            }

            LoadProductPort.ProductInfo productInfo = productInfoOpt.get();

            // 2. 상품 통계 조회
            Optional<LoadProductStatsPort.ProductStatsInfo> statsInfoOpt = 
                loadProductStatsPort.loadProductStatsByProductId(command.getProductId());

            // 3. 결과 생성
            GetProductDetailUseCase.GetProductDetailResult result = new GetProductDetailUseCase.GetProductDetailResult(
                    productInfo.getId(),
                    productInfo.getName(),
                    productInfo.getCurrentPrice(),
                    productInfo.getStock(),
                    "ACTIVE", // 기본 상태
                    null, // createdAt
                    null  // updatedAt
            );

            return Optional.of(result);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 인기 상품 조회 (Facade 메서드)
     */
    public GetPopularProductsUseCase.GetPopularProductsResult getPopularProducts(GetPopularProductsUseCase.GetPopularProductsCommand command) {
        try {
            // 1. 인기 상품 통계 조회
            List<LoadProductStatsPort.ProductStatsInfo> popularStats = loadProductStatsPort.loadTopProductsBySales(5);

            // 2. 상품 상세 정보 조회 및 결과 생성
            List<GetPopularProductsUseCase.PopularProductInfo> popularProducts = popularStats.stream()
                    .map(this::enrichProductStatsInfo)
                    .collect(Collectors.toList());

            return new GetPopularProductsUseCase.GetPopularProductsResult(popularProducts);

        } catch (Exception e) {
            return new GetPopularProductsUseCase.GetPopularProductsResult(List.of());
        }
    }

    /**
     * 상품 통계 정보 보강
     */
    private GetPopularProductsUseCase.PopularProductInfo enrichProductStatsInfo(LoadProductStatsPort.ProductStatsInfo statsInfo) {
        // 상품 상세 정보 조회
        Optional<LoadProductPort.ProductInfo> productInfoOpt = loadProductPort.loadProductById(statsInfo.getProductId());
        
        String productName = productInfoOpt.map(LoadProductPort.ProductInfo::getName).orElse("알 수 없는 상품");
        Integer currentPrice = productInfoOpt.map(LoadProductPort.ProductInfo::getCurrentPrice).orElse(0);

        return new GetPopularProductsUseCase.PopularProductInfo(
                statsInfo.getProductId(),
                productName,
                currentPrice,
                productInfoOpt.map(LoadProductPort.ProductInfo::getStock).orElse(0),
                statsInfo.getTotalSalesCount(),
                statsInfo.getTotalSalesAmount(),
                statsInfo.getRecentSalesCount(),
                statsInfo.getRecentSalesAmount(),
                statsInfo.getConversionRate(),
                null, // lastOrderDate
                statsInfo.getRank()
        );
    }
} 