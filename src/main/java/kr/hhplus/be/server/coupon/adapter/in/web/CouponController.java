package kr.hhplus.be.server.coupon.adapter.in.web;

import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.response.ErrorResponse;
import kr.hhplus.be.server.coupon.application.response.CouponResponse;
import kr.hhplus.be.server.coupon.application.response.UserCouponResponse;
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
@RequestMapping("/api/coupons")
@Tag(name = "Coupon", description = "쿠폰 관리 API")
public class CouponController {

    private final IssueCouponUseCase issueCouponUseCase;
    private final GetUserCouponsUseCase getUserCouponsUseCase;

    public CouponController(IssueCouponUseCase issueCouponUseCase, GetUserCouponsUseCase getUserCouponsUseCase) {
        this.issueCouponUseCase = issueCouponUseCase;
        this.getUserCouponsUseCase = getUserCouponsUseCase;
    }

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
            IssueCouponUseCase.IssueCouponCommand command = new IssueCouponUseCase.IssueCouponCommand(userId, id);
            IssueCouponUseCase.IssueCouponResult result = issueCouponUseCase.issueCoupon(command);

            if (!result.isSuccess()) {
                return ResponseEntity.badRequest().body(new ErrorResponse(result.getErrorMessage()));
            }

            CouponResponse response = new CouponResponse(
                    result.getUserCouponId(),
                    result.getCouponId(),
                    result.getCouponName(),
                    result.getDiscountAmount(),
                    result.getStatus(),
                    result.getIssuedAt()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("쿠폰 발급 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

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
            GetUserCouponsUseCase.GetUserCouponsCommand command = new GetUserCouponsUseCase.GetUserCouponsCommand(userId);
            GetUserCouponsUseCase.GetUserCouponsResult result = getUserCouponsUseCase.getUserCoupons(command);

            List<UserCouponResponse> responses = result.getUserCoupons().stream()
                    .map(userCoupon -> new UserCouponResponse(
                            userCoupon.getUserCouponId(),
                            userCoupon.getCouponId(),
                            userCoupon.getCouponName(),
                            userCoupon.getDiscountAmount(),
                            userCoupon.getStatus(),
                            userCoupon.getIssuedAt(),
                            userCoupon.getUsedAt()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("쿠폰 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
} 