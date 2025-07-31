package kr.hhplus.be.server.order.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 API 문서화 인터페이스
 * Swagger 어노테이션만 포함하여 API 문서화를 담당
 */
@Tag(name = "Order", description = "주문 관리 API")
public interface OrderApiDocumentation {

    @PostMapping
    @Operation(summary = "주문 생성", description = "새로운 주문을 생성하고 결제를 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주문 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 주문 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<?> createOrder(
            @Parameter(description = "주문 요청", required = true)
            @RequestBody kr.hhplus.be.server.order.adapter.in.dto.OrderRequest request);
} 