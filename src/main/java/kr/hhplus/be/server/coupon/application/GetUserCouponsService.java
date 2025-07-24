package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.facade.CouponFacade;

import org.springframework.stereotype.Service;

/**
 * 사용자 쿠폰 조회 Application 서비스 (Facade 패턴 적용)
 */
@Service
public class GetUserCouponsService implements GetUserCouponsUseCase {

    private final CouponFacade couponFacade;

    public GetUserCouponsService(CouponFacade couponFacade) {
        this.couponFacade = couponFacade;
    }

    @Override
    public GetUserCouponsResult getUserCoupons(GetUserCouponsCommand command) {
        return couponFacade.getUserCoupons(command);
    }
} 