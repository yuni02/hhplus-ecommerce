package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.coupon.domain.CouponDomainService;
import kr.hhplus.be.server.coupon.domain.CouponRepository;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.domain.UserCouponRepository;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 UseCase
 * 외부 의존성 없이 도메인 서비스만 호출
 */
@Component
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    public IssueCouponUseCase(CouponRepository couponRepository,
                             UserCouponRepository userCouponRepository,
                             UserRepository userRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.userRepository = userRepository;
    }

    public Output execute(Input input) {
        // 사용자 존재 확인
        if (!userRepository.existsById(input.userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 쿠폰 존재 확인
        Coupon coupon = couponRepository.findById(input.couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 중복 발급 확인
        if (CouponDomainService.isAlreadyIssued(input.userId, input.couponId, 
                userCouponRepository.existsByUserIdAndCouponId(input.userId, input.couponId))) {
            throw new IllegalArgumentException("이미 발급받은 쿠폰입니다.");
        }

        // 쿠폰 발급 가능 여부 확인
        if (!CouponDomainService.canIssueCoupon(coupon)) {
            throw new IllegalStateException("쿠폰 발급이 마감되었습니다.");
        }

        // 도메인 서비스를 통한 쿠폰 발급 처리
        coupon = CouponDomainService.issueCoupon(coupon);
        couponRepository.save(coupon);

        // 사용자 쿠폰 생성
        UserCoupon userCoupon = CouponDomainService.createUserCoupon(input.userId, input.couponId);
        userCoupon = userCouponRepository.save(userCoupon);

        return new Output(
            userCoupon.getId(),
            userCoupon.getCouponId(),
            coupon.getName(),
            coupon.getDiscountAmount().intValue(),
            userCoupon.getStatus().name(),
            userCoupon.getIssuedAt(),
            userCoupon.getUsedAt()
        );
    }

    public static class Input {
        private final Long userId;
        private final Long couponId;

        public Input(Long userId, Long couponId) {
            this.userId = userId;
            this.couponId = couponId;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getCouponId() {
            return couponId;
        }
    }

    public static class Output {
        private final Long userCouponId;
        private final Long couponId;
        private final String couponName;
        private final Integer discountAmount;
        private final String status;
        private final LocalDateTime issuedAt;
        private final LocalDateTime usedAt;

        public Output(Long userCouponId, Long couponId, String couponName,
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