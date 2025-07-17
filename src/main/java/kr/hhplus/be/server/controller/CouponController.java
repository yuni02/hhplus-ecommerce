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

}