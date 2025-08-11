package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 Application 서비스
 */
@Service
public class IssueCouponService implements IssueCouponUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadCouponPort loadCouponPort;
    private final SaveUserCouponPort saveUserCouponPort;

    public IssueCouponService(LoadUserPort loadUserPort,
                             LoadCouponPort loadCouponPort,
                             SaveUserCouponPort saveUserCouponPort) {
        this.loadUserPort = loadUserPort;
        this.loadCouponPort = loadCouponPort;
        this.saveUserCouponPort = saveUserCouponPort;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public IssueCouponResult issueCoupon(IssueCouponCommand command) {
        try {
            // 1. 입력값 검증
            if (command.getUserId() == null || command.getUserId() <= 0) {
                return IssueCouponResult.failure("잘못된 사용자 ID입니다.");
            }
            
            // 2. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return IssueCouponResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 3. 쿠폰 정보를 락과 함께 조회 (선착순 확인)
            LoadCouponPort.CouponInfo couponInfo = loadCouponPort.loadCouponByIdWithLock(command.getCouponId())
                    .orElse(null);
            
            if (couponInfo == null) {
                return IssueCouponResult.failure("존재하지 않는 쿠폰입니다.");
            }

            // 4. 쿠폰 발급 가능 여부 확인
            if (!canIssueCoupon(couponInfo)) {
                return IssueCouponResult.failure("발급할 수 없는 쿠폰입니다.");
            }

            // 5. 쿠폰 발급 수량을 원자적으로 증가 (선착순 처리)
            if (!loadCouponPort.incrementIssuedCount(command.getCouponId())) {
                return IssueCouponResult.failure("쿠폰이 모두 소진되었습니다. 선착순 발급에 실패했습니다.");
            }

            // 6. 사용자 쿠폰 생성
            LocalDateTime now = LocalDateTime.now();
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(command.getUserId())
                    .couponId(command.getCouponId())
                    .discountAmount(couponInfo.getDiscountAmount())
                    .issuedAt(now)
                    .build();
            
            UserCoupon savedUserCoupon = saveUserCouponPort.saveUserCoupon(userCoupon);

            return IssueCouponResult.success(
                    savedUserCoupon.getId(),
                    savedUserCoupon.getCouponId(),
                    couponInfo.getName(),

                    couponInfo.getDiscountAmount(),
                    savedUserCoupon.getStatus().name(),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            return IssueCouponResult.failure("쿠폰 발급 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 쿠폰 발급 가능 여부 확인
     */
    private boolean canIssueCoupon(LoadCouponPort.CouponInfo couponInfo) {
        // ACTIVE 상태이고, 발급 수량이 최대치에 도달하지 않은 경우에만 발급 가능
        return "ACTIVE".equals(couponInfo.getStatus()) && 
               couponInfo.getIssuedCount() < couponInfo.getMaxIssuanceCount();
    }
} 