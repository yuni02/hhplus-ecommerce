package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.coupon.domain.UserCouponRepository;
import kr.hhplus.be.server.user.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public List<UserCoupon> execute(Long userId) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        
        return userCouponRepository.findByUserId(userId);
    }
} 