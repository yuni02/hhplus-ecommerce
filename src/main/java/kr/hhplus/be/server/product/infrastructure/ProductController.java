package kr.hhplus.be.server.product.infrastructure;

import kr.hhplus.be.server.dto.response.ProductResponse;
import kr.hhplus.be.server.dto.response.PopularProductStatsResponse;
import kr.hhplus.be.server.product.application.GetAllProductsUseCase;
import kr.hhplus.be.server.product.application.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.domain.ProductStats;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "상품 관리 API")
public class ProductController {

    private final GetAllProductsUseCase getAllProductsUseCase;
    private final GetPopularProductsUseCase getPopularProductsUseCase;

    public ProductController(GetAllProductsUseCase getAllProductsUseCase,
                           GetPopularProductsUseCase getPopularProductsUseCase) {
        this.getAllProductsUseCase = getAllProductsUseCase;
        this.getPopularProductsUseCase = getPopularProductsUseCase;
    }

    /**
     * 상품 목록 조회 API
     */
    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "등록된 모든 상품의 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getAllProducts() {
        try {
            List<Product> products = getAllProductsUseCase.execute();

            List<ProductResponse> responses = products.stream()
                    .map(product -> new ProductResponse(
                            product.getId(),
                            product.getName(),
                            product.getCurrentPrice().intValue(),
                            product.getStock(),
                            product.getStatus().name(),
                            product.getCreatedAt(),
                            product.getUpdatedAt()
                    ))
                    .collect(Collectors.toList());

            if (responses.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "등록된 상품이 없습니다.",
                        "products", responses));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "상품 목록 조회 성공",
                    "products", responses));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "상품 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 인기 상품 조회 API
     */
    @GetMapping("/popular")
    @Operation(summary = "인기 상품 조회", description = "최근 3일간 판매량 기준 상위 5개 인기 상품을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getPopularProducts() {
        try {
            List<ProductStats> popularProductStats = getPopularProductsUseCase.execute(5);

            if (popularProductStats.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "인기 상품 데이터가 없습니다.",
                        "stats", Map.of(
                                "aggregationPeriod", "최근 3일",
                                "totalProducts", 0,
                                "lastUpdated", LocalDateTime.now()),
                        "popularProducts", popularProductStats));
            }

            // 집계 통계 계산
            int totalRecentSales = popularProductStats.stream()
                    .mapToInt(ProductStats::getRecentSalesCount)
                    .sum();
            long totalRecentRevenue = popularProductStats.stream()
                    .mapToLong(stats -> stats.getRecentSalesAmount().longValue())
                    .sum();

            List<PopularProductStatsResponse> responses = popularProductStats.stream()
                    .map(stats -> new PopularProductStatsResponse(
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

            return ResponseEntity.ok(Map.of(
                    "message", "인기 상품 조회 성공",
                    "stats", Map.of(
                            "aggregationPeriod", "최근 3일",
                            "criteriaDescription", "판매량 기준 상위 5개 상품",
                            "totalProducts", popularProductStats.size(),
                            "totalRecentSales", totalRecentSales,
                            "totalRecentRevenue", totalRecentRevenue,
                            "lastUpdated", LocalDateTime.now()),
                    "popularProducts", responses.stream().map(product -> Map.of(
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
                            .collect(Collectors.toList())));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "인기 상품 조회 중 오류가 발생했습니다."));
        }
    }
}