package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.UpdateUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 쿠폰 사용 Application 서비스
 */
@Service
public class UseCouponService implements UseCouponUseCase {

    private final LoadUserCouponPort loadUserCouponPort;
    private final UpdateUserCouponPort updateUserCouponPort;

    public UseCouponService(LoadUserCouponPort loadUserCouponPort,
                           UpdateUserCouponPort updateUserCouponPort) {
        this.loadUserCouponPort = loadUserCouponPort;
        this.updateUserCouponPort = updateUserCouponPort;
    }

    @Override
    @Transactional
    public UseCouponResult useCoupon(UseCouponCommand command) {
        try {
            // 1. 사용자 쿠폰 조회
            var userCouponOpt = loadUserCouponPort.loadUserCoupon(command.getUserCouponId());
            
            if (userCouponOpt.isEmpty()) {
                return UseCouponResult.failure("쿠폰을 찾을 수 없습니다.");
            }

            UserCoupon userCoupon = userCouponOpt.get();

            // 2. 쿠폰 소유자 확인
            if (!userCoupon.getUserId().equals(command.getUserId())) {
                return UseCouponResult.failure("해당 쿠폰의 소유자가 아닙니다.");
            }

            // 3. 쿠폰 상태 확인
            if (!userCoupon.isAvailable()) {
                return UseCouponResult.failure("사용할 수 없는 쿠폰입니다.");
            }

            // 4. 할인 금액 계산
            BigDecimal discountAmount = BigDecimal.valueOf(userCoupon.getDiscountAmount());
            BigDecimal discountedAmount = command.getOrderAmount().subtract(discountAmount);

            // 5. 최소 주문 금액 확인 (할인 후 금액이 음수가 되지 않도록)
            if (discountedAmount.compareTo(BigDecimal.ZERO) < 0) {
                discountedAmount = BigDecimal.ZERO;
            }

            // 6. 쿠폰 사용 처리 (도메인 로직)
            userCoupon.use(LocalDateTime.now());
            updateUserCouponPort.updateUserCoupon(userCoupon);

            return UseCouponResult.success(discountedAmount, userCoupon.getDiscountAmount());
            
        } catch (Exception e) {
            return UseCouponResult.failure("쿠폰 사용 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 