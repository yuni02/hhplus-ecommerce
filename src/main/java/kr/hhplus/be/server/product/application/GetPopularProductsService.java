package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.application.port.out.LoadProductPort;
import kr.hhplus.be.server.product.application.port.out.LoadProductStatsPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 인기 상품 조회 Application 서비스
 */
@Service
public class GetPopularProductsService implements GetPopularProductsUseCase {

    private final LoadProductStatsPort loadProductStatsPort;
    private final LoadProductPort loadProductPort;

    public GetPopularProductsService(LoadProductStatsPort loadProductStatsPort, LoadProductPort loadProductPort) {
        this.loadProductStatsPort = loadProductStatsPort;
        this.loadProductPort = loadProductPort;
    }

    @Override
    public GetPopularProductsResult getPopularProducts(GetPopularProductsCommand command) {
        // 1. 상품 통계 조회
        List<LoadProductStatsPort.ProductStatsInfo> allStats = loadProductStatsPort.loadAllProductStats();
        
        // 2. 인기 순위 계산 (최근 판매량 기준)
        List<LoadProductStatsPort.ProductStatsInfo> popularStats = allStats.stream()
                .sorted(Comparator.comparing(LoadProductStatsPort.ProductStatsInfo::getRecentSalesCount).reversed())
                .limit(command.getLimit())
                .collect(Collectors.toList());
        
        // 3. 순위 설정
        for (int i = 0; i < popularStats.size(); i++) {
            // 순위는 이미 ProductStatsInfo에 포함되어 있음
        }
        
        // 4. 상품 정보와 통합하여 결과 생성
        List<PopularProductInfo> popularProducts = popularStats.stream()
                .map(stats -> {
                    // 상품 정보 조회 (실제로는 캐시나 조인으로 최적화 가능)
                    LoadProductPort.ProductInfo productInfo = loadProductPort.loadProductById(stats.getProductId())
                            .orElse(null);
                    
                    return new PopularProductInfo(
                            stats.getProductId(),
                            stats.getProductName(),
                            productInfo != null ? productInfo.getCurrentPrice() : 0,
                            productInfo != null ? productInfo.getStock() : 0,
                            stats.getTotalSalesCount(),
                            stats.getTotalSalesAmount(),
                            stats.getRecentSalesCount(),
                            stats.getRecentSalesAmount(),
                            stats.getConversionRate(),
                            LocalDateTime.now(), // 실제로는 stats에서 가져와야 함
                            stats.getRank()
                    );
                })
                .collect(Collectors.toList());
        
        return new GetPopularProductsResult(popularProducts);
    }
} 