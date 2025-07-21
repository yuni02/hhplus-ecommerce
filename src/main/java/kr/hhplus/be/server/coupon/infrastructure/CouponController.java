package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.dto.response.UserCouponResponse;
import kr.hhplus.be.server.coupon.application.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "Coupon", description = "쿠폰 관리 API")
public class CouponController {

    private final IssueCouponUseCase issueCouponUseCase;
    private final GetUserCouponsUseCase getUserCouponsUseCase;

    public CouponController(IssueCouponUseCase issueCouponUseCase, 
                           GetUserCouponsUseCase getUserCouponsUseCase) {
        this.issueCouponUseCase = issueCouponUseCase;
        this.getUserCouponsUseCase = getUserCouponsUseCase;
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
            @Parameter(description = "사용자 ID", required = true, example = "1") @RequestParam Long userId) {
        try {
            // 입력값 검증
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 쿠폰 ID입니다."));
            }
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 사용자 ID입니다."));
            }

            UserCoupon userCoupon = issueCouponUseCase.execute(userId, id);

            // 임시로 간단한 응답 생성 (실제로는 쿠폰 정보도 필요)
            UserCouponResponse response = new UserCouponResponse(
                    userCoupon.getId(),
                    userCoupon.getCouponId(),
                    "쿠폰", // 실제로는 쿠폰 정보에서 가져와야 함
                    1000, // 실제로는 쿠폰 정보에서 가져와야 함
                    userCoupon.getStatus().name(),
                    userCoupon.getIssuedAt(),
                    userCoupon.getUsedAt()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "쿠폰이 성공적으로 발급되었습니다.",
                    "userCoupon", response));

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
            // 입력값 검증
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않은 사용자 ID입니다."));
            }

            List<UserCoupon> userCoupons = getUserCouponsUseCase.execute(userId);

            List<UserCouponResponse> responses = userCoupons.stream()
                    .map(uc -> new UserCouponResponse(
                            uc.getId(),
                            uc.getCouponId(),
                            "쿠폰", // 실제로는 쿠폰 정보에서 가져와야 함
                            1000, // 실제로는 쿠폰 정보에서 가져와야 함
                            uc.getStatus().name(),
                            uc.getIssuedAt(),
                            uc.getUsedAt()
                    ))
                    .collect(Collectors.toList());

            if (responses.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "보유한 쿠폰이 없습니다.",
                        "userCoupons", responses));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "보유 쿠폰 조회 성공",
                    "userCoupons", responses));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "보유 쿠폰 조회 중 오류가 발생했습니다."));
        }
    }
} 