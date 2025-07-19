package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.response.ProductResponse;
import kr.hhplus.be.server.service.DummyDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.hhplus.be.server.dto.response.PopularProductStatsResponse;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final DummyDataService dummyDataService;

    public ProductController(DummyDataService dummyDataService) {
        this.dummyDataService = dummyDataService;
    }

    /**
     * 상품 목록 조회 API
     */
    @GetMapping
    public ResponseEntity<?> getAllProducts() {
        try {
            List<ProductResponse> products = dummyDataService.getAllProducts();

            if (products.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "등록된 상품이 없습니다.",
                        "products", products));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "상품 목록 조회 성공",
                    "products", products));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "상품 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 인기 상품 조회 API
     */
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularProducts() {
        try {
            List<PopularProductStatsResponse> popularProductStats = dummyDataService.getPopularProductStats();

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
                    .mapToInt(PopularProductStatsResponse::getRecentSalesCount)
                    .sum();
            long totalRecentRevenue = popularProductStats.stream()
                    .mapToLong(PopularProductStatsResponse::getRecentSalesAmount)
                    .sum();

            return ResponseEntity.ok(Map.of(
                    "message", "인기 상품 조회 성공",
                    "stats", Map.of(
                            "aggregationPeriod", "최근 3일",
                            "criteriaDescription", "판매량 기준 상위 5개 상품",
                            "totalProducts", popularProductStats.size(),
                            "totalRecentSales", totalRecentSales,
                            "totalRecentRevenue", totalRecentRevenue,
                            "lastUpdated", LocalDateTime.now()),
                    "popularProducts", popularProductStats.stream().map(product -> Map.of(
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