package kr.hhplus.be.server.coupon.infrastructure;

import kr.hhplus.be.server.coupon.application.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.GetUserCouponsUseCase;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CouponAdapter {

    public IssueCouponUseCase.Input adaptIssueRequest(Long couponId, Long userId) {
        // 입력값 검증
        if (couponId == null || couponId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 쿠폰 ID입니다.");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }

        return new IssueCouponUseCase.Input(userId, couponId);
    }

    public Map<String, Object> adaptIssueResponse(IssueCouponUseCase.Output output) {
        return Map.of(
                "message", "쿠폰이 성공적으로 발급되었습니다.",
                "userCoupon", Map.of(
                        "userCouponId", output.getUserCouponId(),
                        "couponId", output.getCouponId(),
                        "couponName", output.getCouponName(),
                        "discountAmount", output.getDiscountAmount(),
                        "status", output.getStatus(),
                        "issuedAt", output.getIssuedAt(),
                        "usedAt", output.getUsedAt()
                ));
    }

    public GetUserCouponsUseCase.Input adaptGetUserCouponsRequest(Long userId) {
        // 입력값 검증
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }

        return new GetUserCouponsUseCase.Input(userId);
    }

    public Map<String, Object> adaptGetUserCouponsResponse(GetUserCouponsUseCase.Output output) {
        var userCouponResponses = output.getUserCoupons().stream()
                .map(uc -> Map.of(
                        "userCouponId", uc.getUserCouponId(),
                        "couponId", uc.getCouponId(),
                        "couponName", uc.getCouponName(),
                        "discountAmount", uc.getDiscountAmount(),
                        "status", uc.getStatus(),
                        "issuedAt", uc.getIssuedAt(),
                        "usedAt", uc.getUsedAt()
                ))
                .collect(Collectors.toList());

        if (userCouponResponses.isEmpty()) {
            return Map.of(
                    "message", "보유한 쿠폰이 없습니다.",
                    "userCoupons", userCouponResponses);
        }

        return Map.of(
                "message", "보유 쿠폰 조회 성공",
                "userCoupons", userCouponResponses);
    }
} 