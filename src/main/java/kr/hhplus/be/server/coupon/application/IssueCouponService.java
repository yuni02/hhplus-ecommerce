package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.facade.CouponFacade;

import org.springframework.stereotype.Service;

/**
 * 쿠폰 발급 Application 서비스 (Facade 패턴 적용)
 */
@Service
public class IssueCouponService implements IssueCouponUseCase {

    private final CouponFacade couponFacade;

    public IssueCouponService(CouponFacade couponFacade) {
        this.couponFacade = couponFacade;
    }

    @Override
    public IssueCouponResult issueCoupon(IssueCouponCommand command) {
        return couponFacade.issueCoupon(command);
    }
} 