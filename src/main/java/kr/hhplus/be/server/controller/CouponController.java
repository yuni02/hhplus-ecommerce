package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.request.CouponRequest;
import kr.hhplus.be.server.dto.response.CouponResponse;
import kr.hhplus.be.server.dto.response.UserCouponResponse;
import kr.hhplus.be.server.service.DummyDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final DummyDataService dummyDataService;

    public CouponController(DummyDataService dummyDataService) {
        this.dummyDataService = dummyDataService;
    }

    /**
     * 발급 가능한 쿠폰 조회 API
     */
    @GetMapping
    public ResponseEntity<?> getAvailableCoupons() {
        try {
            List<CouponResponse> coupons = dummyDataService.getAvailableCoupons();

            if (coupons.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "현재 발급 가능한 쿠폰이 없습니다.",
                        "coupons", coupons));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "발급 가능한 쿠폰 조회 성공",
                    "coupons", coupons));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "쿠폰 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 쿠폰 발급 API (선착순)
     */
    @PostMapping("/{id}/issue")
    public ResponseEntity<?> issueCoupon(@PathVariable Long id, @RequestParam Long userId) {
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
    public ResponseEntity<?> getUserCoupons(@PathVariable Long userId) {
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

    /**
     * 쿠폰 생성 API (관리자)
     */
    @PostMapping
    public ResponseEntity<?> createCoupon(@RequestBody CouponRequest request) {
        try {
            // 입력값 검증
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "쿠폰명은 필수입니다."));
            }
            if (request.getDiscountAmount() == null || request.getDiscountAmount() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "할인 금액은 양수여야 합니다."));
            }
            if (request.getTotalQuantity() == null || request.getTotalQuantity() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "총 발급 수량은 양수여야 합니다."));
            }

            CouponResponse coupon = dummyDataService.createCoupon(
                    request.getName(),
                    request.getDiscountAmount(),
                    request.getTotalQuantity());

            return ResponseEntity.ok(Map.of(
                    "message", "쿠폰이 성공적으로 생성되었습니다.",
                    "coupon", coupon));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "쿠폰 생성 중 오류가 발생했습니다."));
        }
    }

}