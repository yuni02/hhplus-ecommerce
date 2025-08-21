package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.GetActiveCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 활성 쿠폰 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetActiveCouponsService implements GetActiveCouponsUseCase {

    private final LoadCouponPort loadCouponPort;

    @Override
    public GetActiveCouponsResult getActiveCoupons(GetActiveCouponsCommand command) {
        try {
            // 현재 시간 기준으로 활성 쿠폰 조회
            LocalDateTime now = LocalDateTime.now();
            
            List<LoadCouponPort.CouponInfo> allCoupons = loadCouponPort.loadAllCoupons();
            
            List<ActiveCouponInfo> activeCoupons = allCoupons.stream()
                .filter(coupon -> isActiveCoupon(coupon, now))
                .map(this::mapToActiveCouponInfo)
                .collect(Collectors.toList());
            
            log.debug("활성 쿠폰 조회 완료 - count: {}", activeCoupons.size());
            return new GetActiveCouponsResult(activeCoupons);
            
        } catch (Exception e) {
            log.error("활성 쿠폰 조회 중 오류 발생", e);
            return new GetActiveCouponsResult(List.of());
        }
    }

    /**
     * 쿠폰이 활성 상태인지 확인
     */
    private boolean isActiveCoupon(LoadCouponPort.CouponInfo coupon, LocalDateTime now) {
        return "ACTIVE".equals(coupon.getStatus()) &&
               coupon.getValidFrom() != null && coupon.getValidFrom().isBefore(now) &&
               coupon.getValidTo() != null && coupon.getValidTo().isAfter(now);
    }

    /**
     * CouponInfo를 ActiveCouponInfo로 변환
     */
    private ActiveCouponInfo mapToActiveCouponInfo(LoadCouponPort.CouponInfo coupon) {
        return new ActiveCouponInfo(
            coupon.getCouponId(),
            coupon.getCouponName(),
            coupon.getMaxIssuanceCount(),
            coupon.getValidFrom(),
            coupon.getValidTo(),
            coupon.getStatus()
        );
    }
}
