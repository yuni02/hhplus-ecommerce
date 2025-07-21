package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.domain.ProductDomainService;
import kr.hhplus.be.server.product.domain.ProductStats;
import kr.hhplus.be.server.product.domain.ProductStatsRepository;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public List<ProductStats> execute(int limit) {
        // 도메인 서비스를 통한 인기 상품 순위 계산
        List<ProductStats> allStats = productStatsRepository.findAll();
        return ProductDomainService.calculatePopularityRanking(allStats, limit);
    }
}