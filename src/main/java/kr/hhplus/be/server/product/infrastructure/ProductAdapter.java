package kr.hhplus.be.server.product.infrastructure;

import kr.hhplus.be.server.product.application.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.GetPopularProductsUseCase;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class ProductAdapter {

    public GetProductDetailUseCase.Input adaptGetProductDetailRequest(Long productId) {
        // 입력값 검증
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 상품 ID입니다.");
        }

        return new GetProductDetailUseCase.Input(productId);
    }

    public Map<String, Object> adaptGetProductDetailResponse(GetProductDetailUseCase.Output output) {
        return Map.of(
                "message", "상품 상세 조회 성공",
                "product", Map.of(
                        "id", output.getId(),
                        "name", output.getName(),
                        "currentPrice", output.getCurrentPrice(),
                        "stock", output.getStock(),
                        "status", output.getStatus(),
                        "createdAt", output.getCreatedAt(),
                        "updatedAt", output.getUpdatedAt()
                ));
    }

    public GetPopularProductsUseCase.Input adaptGetPopularProductsRequest(int limit) {
        // 입력값 검증
        if (limit <= 0) {
            throw new IllegalArgumentException("조회 개수는 양수여야 합니다.");
        }

        return new GetPopularProductsUseCase.Input(limit);
    }

    public Map<String, Object> adaptGetPopularProductsResponse(GetPopularProductsUseCase.Output output) {
        var popularProducts = output.getPopularProducts().stream()
                .map(product -> Map.of(
                        "rank", product.getRank(),
                        "productId", product.getProductId(),
                        "productName", product.getProductName(),
                        "currentPrice", product.getCurrentPrice(),
                        "stock", product.getStock(),
                        "salesData", Map.of(
                                "recentSalesCount", product.getRecentSalesCount(),
                                "recentSalesAmount", product.getRecentSalesAmount(),
                                "totalSalesCount", product.getTotalSalesCount(),
                                "totalSalesAmount", product.getTotalSalesAmount()),
                        "performance", Map.of(
                                "conversionRate", String.format("%.2f%%", product.getConversionRate()),
                                "lastOrderDate", product.getLastOrderDate())))
                .toList();

        if (popularProducts.isEmpty()) {
            return Map.of(
                    "message", "인기 상품 데이터가 없습니다.",
                    "stats", Map.of(
                            "aggregationPeriod", "최근 3일",
                            "totalProducts", 0,
                            "lastUpdated", LocalDateTime.now()),
                    "popularProducts", popularProducts);
        }

        // 집계 통계 계산
        int totalRecentSales = output.getPopularProducts().stream()
                .mapToInt(GetPopularProductsUseCase.PopularProductOutput::getRecentSalesCount)
                .sum();
        long totalRecentRevenue = output.getPopularProducts().stream()
                .mapToLong(GetPopularProductsUseCase.PopularProductOutput::getRecentSalesAmount)
                .sum();

        return Map.of(
                "message", "인기 상품 조회 성공",
                "stats", Map.of(
                        "aggregationPeriod", "최근 3일",
                        "criteriaDescription", "판매량 기준 상위 5개 상품",
                        "totalProducts", output.getPopularProducts().size(),
                        "totalRecentSales", totalRecentSales,
                        "totalRecentRevenue", totalRecentRevenue,
                        "lastUpdated", LocalDateTime.now()),
                "popularProducts", popularProducts);
    }
} 