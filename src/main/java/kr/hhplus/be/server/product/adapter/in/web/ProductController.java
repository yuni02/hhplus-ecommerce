package kr.hhplus.be.server.product.adapter.in.web;

import kr.hhplus.be.server.product.application.facade.ProductFacade;
import kr.hhplus.be.server.product.application.port.in.GetProductDetailUseCase;
import kr.hhplus.be.server.product.application.response.PopularProductStatsResponse;
import kr.hhplus.be.server.product.application.response.ProductResponse;
import kr.hhplus.be.server.product.application.port.in.GetPopularProductsUseCase;
import kr.hhplus.be.server.product.application.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "상품 관리 API")
public class ProductController {

    private final ProductFacade productFacade;

    public ProductController(ProductFacade productFacade) {
        this.productFacade = productFacade;
    }

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
            GetProductDetailUseCase.GetProductDetailCommand command = 
                    new GetProductDetailUseCase.GetProductDetailCommand(productId);
            
            var productOpt = productFacade.getProductDetail(command);
            
            if (productOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            GetProductDetailUseCase.GetProductDetailResult result = productOpt.get();
            ProductResponse response = new ProductResponse(
                    result.getId(),
                    result.getName(),
                    result.getCurrentPrice(),
                    result.getStock(),
                    result.getStatus(),
                    result.getCreatedAt(),
                    result.getUpdatedAt()
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("상품 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/popular")
    @Operation(summary = "인기 상품 조회", description = "최근 3일간 판매량 기준 상위 5개 인기 상품을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getPopularProducts() {
        try {
            GetPopularProductsUseCase.GetPopularProductsCommand command = 
                    new GetPopularProductsUseCase.GetPopularProductsCommand(5);
            
            GetPopularProductsUseCase.GetPopularProductsResult result = 
                    productFacade.getPopularProducts(command);
            
            List<PopularProductStatsResponse> responses = result.getPopularProducts().stream()
                    .map(product -> new PopularProductStatsResponse(
                            product.getProductId(),
                            product.getProductName(),
                            product.getCurrentPrice(),
                            product.getStock(),
                            product.getTotalSalesCount(),
                            product.getTotalSalesAmount(),
                            product.getRecentSalesCount(),
                            product.getRecentSalesAmount(),
                            product.getConversionRate(),
                            product.getLastOrderDate(),
                            product.getRank()
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("인기 상품 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
} 