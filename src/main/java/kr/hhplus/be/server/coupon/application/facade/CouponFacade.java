package kr.hhplus.be.server.coupon.application.facade;

import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.UpdateUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 쿠폰 관리 Facade
 * 복잡한 쿠폰 관련 로직을 단순화하여 제공
 */
@Service
public class CouponFacade {

    private final LoadUserPort loadUserPort;
    private final LoadCouponPort loadCouponPort;
    private final SaveCouponPort saveCouponPort;        
    private final LoadUserCouponPort loadUserCouponPort;
    private final SaveUserCouponPort saveUserCouponPort;
    private final UpdateUserCouponPort updateUserCouponPort;

    public CouponFacade(LoadUserPort loadUserPort,
                       LoadCouponPort loadCouponPort,
                       SaveCouponPort saveCouponPort,
                       LoadUserCouponPort loadUserCouponPort,
                       SaveUserCouponPort saveUserCouponPort,
                       UpdateUserCouponPort updateUserCouponPort) {
        this.loadUserPort = loadUserPort;
        this.loadCouponPort = loadCouponPort;
        this.saveCouponPort = saveCouponPort;
        this.loadUserCouponPort = loadUserCouponPort;
        this.saveUserCouponPort = saveUserCouponPort;
        this.updateUserCouponPort = updateUserCouponPort;
    }

    /**
     * 쿠폰 발급 (선착순 로직 적용)
     */
    @Transactional
    public IssueCouponUseCase.IssueCouponResult issueCoupon(IssueCouponUseCase.IssueCouponCommand command) {
        try {
            // 1. 입력값 검증
            if (command.getUserId() == null || command.getUserId() <= 0) {
                return IssueCouponUseCase.IssueCouponResult.failure("잘못된 사용자 ID입니다.");
            }
            
            // 2. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return IssueCouponUseCase.IssueCouponResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 3. 쿠폰 정보를 락과 함께 조회 (선착순 확인)
            LoadCouponPort.CouponInfo couponInfo = loadCouponPort.loadCouponByIdWithLock(command.getCouponId())
                    .orElse(null);
            
            if (couponInfo == null) {
                return IssueCouponUseCase.IssueCouponResult.failure("존재하지 않는 쿠폰입니다.");
            }

            // 4. 쿠폰 발급 가능 여부 확인
            if (!canIssueCoupon(couponInfo)) {
                return IssueCouponUseCase.IssueCouponResult.failure("발급할 수 없는 쿠폰입니다.");
            }

            // 5. 쿠폰 발급 수량을 원자적으로 증가 (선착순 처리)
            if (!loadCouponPort.incrementIssuedCount(command.getCouponId())) {
                return IssueCouponUseCase.IssueCouponResult.failure("쿠폰이 모두 소진되었습니다. 선착순 발급에 실패했습니다.");
            }

            // 6. 사용자 쿠폰 생성
            UserCoupon userCoupon = new UserCoupon(
                    command.getUserId(),
                    command.getCouponId(),
                    couponInfo.getDiscountAmount()
            );
            
            UserCoupon savedUserCoupon = saveUserCouponPort.saveUserCoupon(userCoupon);

            return IssueCouponUseCase.IssueCouponResult.success(
                    savedUserCoupon.getId(),
                    savedUserCoupon.getCouponId(),
                    couponInfo.getName(),
                    couponInfo.getDiscountAmount(),
                    savedUserCoupon.getStatus().name(),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            return IssueCouponUseCase.IssueCouponResult.failure("쿠폰 발급 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자 쿠폰 조회 (Facade 메서드)
     */
    public GetUserCouponsUseCase.GetUserCouponsResult getUserCoupons(GetUserCouponsUseCase.GetUserCouponsCommand command) {
        try {
            // 1. 입력값 검증
            if (command.getUserId() == null || command.getUserId() <= 0) {
                throw new IllegalArgumentException("잘못된 사용자 ID입니다.");
            }
            
            // 2. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return new GetUserCouponsUseCase.GetUserCouponsResult(List.of());
            }

            // 3. 사용자 쿠폰 목록 조회
            List<LoadUserCouponPort.UserCouponInfo> userCouponInfos = loadUserCouponPort.loadUserCouponsByUserId(command.getUserId());

            // 4. 쿠폰 상세 정보 조회 및 결과 생성
            List<GetUserCouponsUseCase.UserCouponInfo> result = userCouponInfos.stream()
                    .map(this::enrichUserCouponInfo)
                    .collect(Collectors.toList());

            return new GetUserCouponsUseCase.GetUserCouponsResult(result);

        } catch (Exception e) {
            return new GetUserCouponsUseCase.GetUserCouponsResult(List.of());
        }
    }

    /**
     * 쿠폰 사용 (Facade 메서드)
     */
    @Transactional
    public UseCouponUseCase.UseCouponResult useCoupon(UseCouponUseCase.UseCouponCommand command) {
        try {
            // 1. 사용자 쿠폰 조회
            var userCouponOpt = loadUserCouponPort.loadUserCoupon(command.getUserCouponId());
            
            if (userCouponOpt.isEmpty()) {
                return UseCouponUseCase.UseCouponResult.failure("쿠폰을 찾을 수 없습니다.");
            }

            UserCoupon userCoupon = userCouponOpt.get();

            // 2. 쿠폰 소유자 확인
            if (!userCoupon.getUserId().equals(command.getUserId())) {
                return UseCouponUseCase.UseCouponResult.failure("해당 쿠폰의 소유자가 아닙니다.");
            }

            // 3. 쿠폰 상태 확인
            if (!userCoupon.isAvailable()) {
                return UseCouponUseCase.UseCouponResult.failure("사용할 수 없는 쿠폰입니다.");
            }

            // 4. 할인 금액 계산
            BigDecimal discountAmount = BigDecimal.valueOf(userCoupon.getDiscountAmount());
            BigDecimal discountedAmount = command.getOrderAmount().subtract(discountAmount);

            // 5. 최소 주문 금액 확인 (할인 후 금액이 음수가 되지 않도록)
            if (discountedAmount.compareTo(BigDecimal.ZERO) < 0) {
                discountedAmount = BigDecimal.ZERO;
            }

            // 6. 쿠폰 사용 처리
            userCoupon.use(LocalDateTime.now());
            updateUserCouponPort.updateUserCoupon(userCoupon);

            return UseCouponUseCase.UseCouponResult.success(discountedAmount, userCoupon.getDiscountAmount());
            
        } catch (Exception e) {
            return UseCouponUseCase.UseCouponResult.failure("쿠폰 사용 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 쿠폰 발급 가능 여부 확인 도메인 로직
     */
    private boolean canIssueCoupon(LoadCouponPort.CouponInfo couponInfo) {
        return "ACTIVE".equals(couponInfo.getStatus()) && 
               couponInfo.getIssuedCount() < couponInfo.getMaxIssuanceCount();
    }

    /**
     * 사용자 쿠폰 정보 보강
     */
    private GetUserCouponsUseCase.UserCouponInfo enrichUserCouponInfo(LoadUserCouponPort.UserCouponInfo userCouponInfo) {
        // 쿠폰 상세 정보 조회
        Optional<LoadCouponPort.CouponInfo> couponInfoOpt = loadCouponPort.loadCouponById(userCouponInfo.getCouponId());
        
        String couponName = couponInfoOpt.map(LoadCouponPort.CouponInfo::getName).orElse("알 수 없는 쿠폰");
        Integer discountAmount = couponInfoOpt.map(LoadCouponPort.CouponInfo::getDiscountAmount).orElse(0);
        
        LocalDateTime issuedAt = parseDateTime(userCouponInfo.getIssuedAt());
        LocalDateTime usedAt = userCouponInfo.getUsedAt() != null ? parseDateTime(userCouponInfo.getUsedAt()) : null;

        return new GetUserCouponsUseCase.UserCouponInfo(
                userCouponInfo.getId(),
                userCouponInfo.getCouponId(),
                couponName,
                discountAmount,
                userCouponInfo.getStatus(),
                issuedAt,
                usedAt
        );
    }

    /**
     * 날짜 문자열 파싱
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            return null;
        }
    }
} 