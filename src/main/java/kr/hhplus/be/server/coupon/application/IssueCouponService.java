package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 쿠폰 발급 Application 서비스
 */
@Service
public class IssueCouponService implements IssueCouponUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadCouponPort loadCouponPort;
    private final SaveCouponPort saveCouponPort;
    private final SaveUserCouponPort saveUserCouponPort;
    
    private final AtomicLong userCouponIdGenerator = new AtomicLong(1);

    public IssueCouponService(LoadUserPort loadUserPort, LoadCouponPort loadCouponPort,
                             SaveCouponPort saveCouponPort, SaveUserCouponPort saveUserCouponPort) {
        this.loadUserPort = loadUserPort;
        this.loadCouponPort = loadCouponPort;
        this.saveCouponPort = saveCouponPort;
        this.saveUserCouponPort = saveUserCouponPort;
    }

    @Override
    @Transactional
    public IssueCouponResult issueCoupon(IssueCouponCommand command) {
        try {
            // 1. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return IssueCouponResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 2. 쿠폰 조회
            LoadCouponPort.CouponInfo couponInfo = loadCouponPort.loadCouponById(command.getCouponId())
                    .orElse(null);
            
            if (couponInfo == null) {
                return IssueCouponResult.failure("존재하지 않는 쿠폰입니다.");
            }

            // 3. 쿠폰 발급 가능 여부 확인
            if (!canIssueCoupon(couponInfo)) {
                return IssueCouponResult.failure("발급할 수 없는 쿠폰입니다.");
            }

            // 4. 쿠폰 발급 수량 증가
            SaveCouponPort.CouponInfo updatedCouponInfo = new SaveCouponPort.CouponInfo(
                    couponInfo.getId(),
                    couponInfo.getName(),
                    couponInfo.getDescription(),
                    couponInfo.getDiscountAmount(),
                    couponInfo.getMaxIssuanceCount(),
                    couponInfo.getIssuedCount() + 1,
                    couponInfo.getIssuedCount() + 1 >= couponInfo.getMaxIssuanceCount() ? "SOLD_OUT" : couponInfo.getStatus()
            );
            
            saveCouponPort.saveCoupon(updatedCouponInfo);

            // 5. 사용자 쿠폰 생성
            SaveUserCouponPort.UserCouponInfo userCouponInfo = new SaveUserCouponPort.UserCouponInfo(
                    userCouponIdGenerator.getAndIncrement(),
                    command.getUserId(),
                    command.getCouponId(),
                    "AVAILABLE",
                    LocalDateTime.now().toString(),
                    null,
                    null
            );
            
            SaveUserCouponPort.UserCouponInfo savedUserCoupon = saveUserCouponPort.saveUserCoupon(userCouponInfo);

            return IssueCouponResult.success(
                    savedUserCoupon.getId(),
                    savedUserCoupon.getCouponId(),
                    couponInfo.getName(),
                    couponInfo.getDiscountAmount(),
                    savedUserCoupon.getStatus(),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            return IssueCouponResult.failure("쿠폰 발급 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 쿠폰 발급 가능 여부 확인 도메인 로직
     */
    private boolean canIssueCoupon(LoadCouponPort.CouponInfo couponInfo) {
        return "ACTIVE".equals(couponInfo.getStatus()) && 
               couponInfo.getIssuedCount() < couponInfo.getMaxIssuanceCount();
    }
} 