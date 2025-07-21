package kr.hhplus.be.server.product.infrastructure;

import kr.hhplus.be.server.product.application.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.GetPopularProductsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "상품 관리 API")
public class ProductController {

    private final GetProductDetailUseCase getProductDetailUseCase;
    private final GetPopularProductsUseCase getPopularProductsUseCase;
    private final ProductAdapter productAdapter;

    public ProductController(GetProductDetailUseCase getProductDetailUseCase,
                           GetPopularProductsUseCase getPopularProductsUseCase,
                           ProductAdapter productAdapter) {
        this.getProductDetailUseCase = getProductDetailUseCase;
        this.getPopularProductsUseCase = getPopularProductsUseCase;
        this.productAdapter = productAdapter;
    }

    /**
     * 상품 상세 조회 API
     */
    @GetMapping("/{productId}")
    @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getProductDetail(
            @Parameter(description = "상품 ID", required = true, example = "1") 
            @PathVariable Long productId) {
        try {
            GetProductDetailUseCase.Input input = productAdapter.adaptGetProductDetailRequest(productId);
            var productOpt = getProductDetailUseCase.execute(input);
            
            if (productOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(productAdapter.adaptGetProductDetailResponse(productOpt.get()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "상품 상세 조회 중 오류가 발생했습니다."));
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
            GetPopularProductsUseCase.Input input = productAdapter.adaptGetPopularProductsRequest(5);
            GetPopularProductsUseCase.Output output = getPopularProductsUseCase.execute(input);
            return ResponseEntity.ok(productAdapter.adaptGetPopularProductsResponse(output));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "인기 상품 조회 중 오류가 발생했습니다."));
        }
    }
}