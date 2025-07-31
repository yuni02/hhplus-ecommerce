package kr.hhplus.be.server.product.adapter.in.web;

import kr.hhplus.be.server.product.application.port.in.UpdateProductStatsUseCase;
import kr.hhplus.be.server.product.adapter.in.dto.ProductStatsResponse;
import kr.hhplus.be.server.shared.response.ErrorResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/product-stats")
@Tag(name = "Product Stats", description = "상품 통계 관리 API")
public class ProductStatsController {

    private final UpdateProductStatsUseCase updateProductStatsUseCase;

    public ProductStatsController(UpdateProductStatsUseCase updateProductStatsUseCase) {
        this.updateProductStatsUseCase = updateProductStatsUseCase;
    }

    /**
     * 최근 3일간 상품 통계 업데이트 API
     */
    @PostMapping("/update-recent")
    @Operation(summary = "최근 3일간 상품 통계 업데이트", 
               description = "최근 3일간 가장 많이 팔린 상위 5개 상품 정보를 product_stats 테이블에 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "통계 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 업데이트 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> updateRecentProductStats(
            @Parameter(description = "통계 대상 날짜 (기본값: 오늘)", example = "2025-01-01")
            @RequestParam(value = "targetDate", required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate) {
        
        // 기본값: 오늘 날짜
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        
        UpdateProductStatsUseCase.UpdateProductStatsResult result = 
            updateProductStatsUseCase.updateRecentProductStats(date);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.getErrorMessage()));
        }

        ProductStatsResponse response = new ProductStatsResponse(
            result.getMessage(),
            result.getUpdatedCount(),
            date.toString()
        );

        return ResponseEntity.ok(response);
    }
} 