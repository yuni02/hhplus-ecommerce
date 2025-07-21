package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.application.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.GetUserCouponsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "Coupon", description = "쿠폰 관리 API")
public class CouponController {

    private final IssueCouponUseCase issueCouponUseCase;
    private final GetUserCouponsUseCase getUserCouponsUseCase;
    private final CouponAdapter couponAdapter;

    public CouponController(IssueCouponUseCase issueCouponUseCase,
            GetUserCouponsUseCase getUserCouponsUseCase,
            CouponAdapter couponAdapter) {
        this.issueCouponUseCase = issueCouponUseCase;
        this.getUserCouponsUseCase = getUserCouponsUseCase;
        this.couponAdapter = couponAdapter;
    }

    /**
     * 쿠폰 발급 API (선착순)
     */
    @PostMapping("/{id}/issue")
    @Operation(summary = "쿠폰 발급", description = "선착순으로 쿠폰을 발급받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 발급 불가"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> issueCoupon(
            @Parameter(description = "쿠폰 ID", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "사용자 ID", required = true, example = "1") @RequestParam(required = true) Long userId) {
        try {
            IssueCouponUseCase.Input input = couponAdapter.adaptIssueRequest(id, userId);
            IssueCouponUseCase.Output output = issueCouponUseCase.execute(input);
            return ResponseEntity.ok(couponAdapter.adaptIssueResponse(output));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "쿠폰 발급 중 오류가 발생했습니다."));
        }
    }

    /**
     * 보유 쿠폰 조회 API
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "보유 쿠폰 조회", description = "사용자가 보유한 쿠폰 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getUserCoupons(
            @Parameter(description = "사용자 ID", required = true, example = "1") @PathVariable Long userId) {
        try {
            GetUserCouponsUseCase.Input input = couponAdapter.adaptGetUserCouponsRequest(userId);
            GetUserCouponsUseCase.Output output = getUserCouponsUseCase.execute(input);
            return ResponseEntity.ok(couponAdapter.adaptGetUserCouponsResponse(output));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "보유 쿠폰 조회 중 오류가 발생했습니다."));
        }
    }
}