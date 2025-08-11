package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.shared.service.DistributedLockService;

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
    private final DistributedLockService distributedLockService;

    public IssueCouponService(LoadUserPort loadUserPort,
                             LoadCouponPort loadCouponPort,
                             SaveUserCouponPort saveUserCouponPort,
                             DistributedLockService distributedLockService) {
        this.loadUserPort = loadUserPort;
        this.loadCouponPort = loadCouponPort;
        this.saveUserCouponPort = saveUserCouponPort;
        this.distributedLockService = distributedLockService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public IssueCouponResult issueCoupon(IssueCouponCommand command) {
        String lockKey = DistributedLockService.LockKeyGenerator.couponIssueLock(command.getCouponId());
        boolean lockAcquired = false;
        
        try {
            // 1. 분산락 획득 (쿠폰별 발급 락)
            lockAcquired = distributedLockService.acquireLock(lockKey, 15); // 15초 타임아웃
            if (!lockAcquired) {
                return IssueCouponResult.failure("쿠폰 발급 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 2. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return IssueCouponResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 3. 쿠폰 정보 조회 (비관적 락 적용)
            LoadCouponPort.CouponInfo couponInfo = loadCouponPort.loadCouponByIdWithLock(command.getCouponId())
                    .orElse(null);

            if (couponInfo == null) {
                return IssueCouponResult.failure("쿠폰을 찾을 수 없습니다.");
            }

            // 4. 발급 가능 여부 확인
            if (!canIssueCoupon(couponInfo)) {
                return IssueCouponResult.failure("발급할 수 없는 쿠폰입니다.");
            }

            // 5. 원자적 수량 증가 (비관적 락으로 보호)
            if (!loadCouponPort.incrementIssuedCount(command.getCouponId())) {
                return IssueCouponResult.failure("쿠폰이 모두 소진되었습니다.");
            }

            // 6. 사용자 쿠폰 생성 및 저장
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(command.getUserId())
                    .couponId(command.getCouponId())
                    .discountAmount(couponInfo.getDiscountAmount().intValue())
                    .status(UserCoupon.UserCouponStatus.AVAILABLE)
                    .issuedAt(LocalDateTime.now())
                    .build();

            UserCoupon savedUserCoupon = saveUserCouponPort.saveUserCoupon(userCoupon);

            // 7. 성공 결과 반환
            return IssueCouponResult.success(
                    savedUserCoupon.getId(),
                    couponInfo.getId(),
                    couponInfo.getName(),
                    couponInfo.getDiscountAmount().intValue(),
                    savedUserCoupon.getStatus().name(),
                    savedUserCoupon.getIssuedAt()
            );

        } catch (Exception e) {
            return IssueCouponResult.failure("쿠폰 발급 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            // 8. 분산락 해제
            if (lockAcquired) {
                distributedLockService.releaseLock(lockKey);
            }
        }
    }

    /**
     * 쿠폰 발급 가능 여부 확인
     */
    private boolean canIssueCoupon(LoadCouponPort.CouponInfo couponInfo) {
        return "ACTIVE".equals(couponInfo.getStatus()) &&
               couponInfo.getIssuedCount() < couponInfo.getMaxIssuanceCount();
    }
} 