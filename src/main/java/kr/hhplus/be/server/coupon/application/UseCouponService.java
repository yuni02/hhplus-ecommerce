package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.coupon.application.facade.CouponFacade;

import org.springframework.stereotype.Service;

/**
 * 쿠폰 사용 Application 서비스 (Facade 패턴 적용)
 */
@Service
public class UseCouponService implements UseCouponUseCase {

    private final CouponFacade couponFacade;

    public UseCouponService(CouponFacade couponFacade) {
        this.couponFacade = couponFacade;
    }

    @Override
    public UseCouponResult useCoupon(UseCouponCommand command) {
        return couponFacade.useCoupon(command);
    }
} 