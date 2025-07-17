package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.request.CouponRequest;
import kr.hhplus.be.server.dto.response.CouponResponse;
import kr.hhplus.be.server.dto.response.UserCouponResponse;
import kr.hhplus.be.server.service.DummyDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "Coupon", description = "쿠폰 관리 API")
public class CouponController {

    private final DummyDataService dummyDataService;

    public CouponController(DummyDataService dummyDataService) {
        this.dummyDataService = dummyDataService;
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

            // 사용자 존재 확인
            if (dummyDataService.getUser(userId) == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            UserCouponResponse userCoupon = dummyDataService.issueCoupon(userId, id);

            if (userCoupon == null) {
                CouponResponse coupon = dummyDataService.getCoupon(id);
                if (coupon == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "존재하지 않는 쿠폰입니다."));
                }

                // 중복 발급 체크
                List<UserCouponResponse> userCoupons = dummyDataService.getUserCouponsUpdated(userId);
                boolean alreadyIssued = userCoupons.stream()
                        .anyMatch(uc -> uc.getCouponId().equals(id));

                if (alreadyIssued) {
                    return ResponseEntity.badRequest().body(Map.of("message", "이미 발급받은 쿠폰입니다."));
                }

                return ResponseEntity.badRequest().body(Map.of("message", "쿠폰 발급이 마감되었습니다."));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "쿠폰이 성공적으로 발급되었습니다.",
                    "userCoupon", userCoupon));

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

            // 사용자 존재 확인
            if (dummyDataService.getUser(userId) == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            List<UserCouponResponse> userCoupons = dummyDataService.getUserCouponsUpdated(userId);

            if (userCoupons.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "보유한 쿠폰이 없습니다.",
                        "userCoupons", userCoupons));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "보유 쿠폰 조회 성공",
                    "userCoupons", userCoupons));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "보유 쿠폰 조회 중 오류가 발생했습니다."));
        }
    }

}