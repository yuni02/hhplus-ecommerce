package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.domain.UserCouponRepository;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 쿠폰 조회 UseCase
 * 외부 의존성 없이 도메인 서비스만 호출
 */
@Component
public class GetUserCouponsUseCase {

    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    public GetUserCouponsUseCase(UserCouponRepository userCouponRepository, UserRepository userRepository) {
        this.userCouponRepository = userCouponRepository;
        this.userRepository = userRepository;
    }

    public Output execute(Input input) {
        // 사용자 존재 확인
        if (!userRepository.existsById(input.userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(input.userId);
        
        List<UserCouponOutput> userCouponOutputs = userCoupons.stream()
                .map(uc -> new UserCouponOutput(
                    uc.getId(),
                    uc.getCouponId(),
                    "쿠폰", // 실제로는 쿠폰 정보에서 가져와야 함
                    1000, // 실제로는 쿠폰 정보에서 가져와야 함
                    uc.getStatus().name(),
                    uc.getIssuedAt(),
                    uc.getUsedAt()
                ))
                .collect(Collectors.toList());
        
        return new Output(userCouponOutputs);
    }

    public static class Input {
        private final Long userId;

        public Input(Long userId) {
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }
    }

    public static class Output {
        private final List<UserCouponOutput> userCoupons;

        public Output(List<UserCouponOutput> userCoupons) {
            this.userCoupons = userCoupons;
        }

        public List<UserCouponOutput> getUserCoupons() {
            return userCoupons;
        }
    }

    public static class UserCouponOutput {
        private final Long userCouponId;
        private final Long couponId;
        private final String couponName;
        private final Integer discountAmount;
        private final String status;
        private final LocalDateTime issuedAt;
        private final LocalDateTime usedAt;

        public UserCouponOutput(Long userCouponId, Long couponId, String couponName,
                               Integer discountAmount, String status,
                               LocalDateTime issuedAt, LocalDateTime usedAt) {
            this.userCouponId = userCouponId;
            this.couponId = couponId;
            this.couponName = couponName;
            this.discountAmount = discountAmount;
            this.status = status;
            this.issuedAt = issuedAt;
            this.usedAt = usedAt;
        }

        public Long getUserCouponId() {
            return userCouponId;
        }

        public Long getCouponId() {
            return couponId;
        }

        public String getCouponName() {
            return couponName;
        }

        public Integer getDiscountAmount() {
            return discountAmount;
        }

        public String getStatus() {
            return status;
        }

        public LocalDateTime getIssuedAt() {
            return issuedAt;
        }

        public LocalDateTime getUsedAt() {
            return usedAt;
        }
    }
} 