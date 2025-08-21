package kr.hhplus.be.server.coupon.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.coupon.adapter.in.docs.CouponSchemaDescription;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 쿠폰 API 문서화 인터페이스
 * Swagger 어노테이션만 포함하여 API 문서화를 담당
 */
@Tag(name = "Coupon", description = "쿠폰 관리 API")
public interface CouponApiDocumentation {

    @PostMapping("/{id}/issue")
    @Operation(summary = "쿠폰 발급", description = "선착순으로 쿠폰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 발급 불가"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<?> issueCoupon(
            @Parameter(description = CouponSchemaDescription.couponId, required = true, example = "1") 
            @PathVariable(name = "id") Long id,
            @Parameter(description = CouponSchemaDescription.userId, required = true, example = "1001") 
            @RequestParam(name = "userId", required = true) Long userId,
            @Parameter(description = "즉시 처리 여부", required = false, example = "false") 
            @RequestParam(name = "immediate", required = false, defaultValue = "false") boolean immediate);

    @GetMapping("/users/{userId}")
    @Operation(summary = "보유 쿠폰 조회", description = "사용자가 보유한 쿠폰 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<?> getUserCoupons(
            @Parameter(description = CouponSchemaDescription.userId, required = true, example = "1001") 
            @PathVariable(name = "userId") Long userId);
} 