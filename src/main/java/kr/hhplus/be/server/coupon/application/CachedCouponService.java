package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.UpdateUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CachedCouponService implements GetUserCouponsUseCase, UseCouponUseCase {

    private final LoadCouponPort loadCouponPort;
    private final LoadUserCouponPort loadUserCouponPort;
    private final UpdateUserCouponPort updateUserCouponPort;

    public CachedCouponService(LoadCouponPort loadCouponPort,
                             LoadUserCouponPort loadUserCouponPort,
                             UpdateUserCouponPort updateUserCouponPort) {
        this.loadCouponPort = loadCouponPort;
        this.loadUserCouponPort = loadUserCouponPort;
        this.updateUserCouponPort = updateUserCouponPort;
    }


    /**
     * 사용자 쿠폰 목록 조회 (전체, 캐시 적용) - API 응답용
     */
    @Cacheable(value = "userCouponsAll", key = "#userId", unless = "#result.isEmpty()", cacheManager = "shortTermCacheManager")
    public List<LoadUserCouponPort.UserCouponInfo> getAllUserCoupons(Long userId) {
        log.debug("전체 사용자 쿠폰 목록 조회 (DB) - userId: {}", userId);
        
        List<LoadUserCouponPort.UserCouponInfo> allCoupons = loadUserCouponPort.loadUserCouponsByUserId(userId);
        
        log.debug("All user coupons retrieved - userId: {}, count: {}", userId, allCoupons.size());
        return allCoupons;
    }

    @Override
    public GetUserCouponsResult getUserCoupons(GetUserCouponsCommand command) {
        List<LoadUserCouponPort.UserCouponInfo> userCouponInfos = getAllUserCoupons(command.getUserId());
        
        List<GetUserCouponsUseCase.UserCouponInfo> userCoupons = userCouponInfos.stream()
                .map(info -> {
                    // 쿠폰 정보 조회
                    var coupon = loadCouponPort.loadCouponById(info.getCouponId());
                    
                    return new GetUserCouponsUseCase.UserCouponInfo(
                            info.getId(),
                            info.getCouponId(),
                            coupon.map(c -> c.getName()).orElse("Unknown Coupon"),
                            coupon.map(c -> c.getDiscountAmount()).orElse(0),
                            info.getStatus(),
                            java.time.LocalDateTime.parse(info.getIssuedAt()),
                            info.getUsedAt() != null ? java.time.LocalDateTime.parse(info.getUsedAt()) : null
                    );
                })
                .toList();
        
        return new GetUserCouponsResult(userCoupons);
    }


    @Override
    @CacheEvict(value = "userCouponsAvailable", key = "#command.userId", condition = "#result.success")
    public UseCouponResult useCoupon(UseCouponCommand command) {
        return useCouponWithPessimisticLock(command);
    }

    @Override
    @CacheEvict(value = "userCouponsAvailable", key = "#command.userId", condition = "#result.success")
    public UseCouponResult useCouponWithPessimisticLock(UseCouponCommand command) {
        try {
            // 1. 비관적 락으로 사용자 쿠폰 조회
            Optional<UserCoupon> userCouponOpt = loadUserCouponPort.loadUserCouponWithLock(command.getUserCouponId());
            if (userCouponOpt.isEmpty()) {
                return UseCouponResult.failure("쿠폰을 찾을 수 없습니다.");
            }

            UserCoupon userCoupon = userCouponOpt.get();
            
            // 2. 쿠폰 사용 검증 및 처리
            if (!userCoupon.isAvailable()) {
                return UseCouponResult.failure("사용할 수 없는 쿠폰입니다.");
            }

            // 3. 쿠폰 정보 조회하여 할인 금액 확인
            Optional<LoadCouponPort.CouponInfo> couponInfo = loadCouponPort.loadCouponById(userCoupon.getCouponId());
            if (couponInfo.isEmpty()) {
                return UseCouponResult.failure("쿠폰 정보를 찾을 수 없습니다.");
            }

            Integer discountAmount = couponInfo.get().getDiscountAmount();
            BigDecimal discountedAmount = command.getOrderAmount().subtract(new BigDecimal(discountAmount));

            // 4. 쿠폰 사용 처리
            userCoupon.use(java.time.LocalDateTime.now());
            updateUserCouponPort.updateUserCoupon(userCoupon);

            return UseCouponResult.success(discountedAmount, discountAmount);
            
        } catch (Exception e) {
            log.error("쿠폰 사용 중 오류 발생 (비관적 락) - userCouponId: {}", command.getUserCouponId(), e);
            return UseCouponResult.failure("쿠폰 사용 중 오류가 발생했습니다.");
        }
    }

    @Override
    @CacheEvict(value = "userCouponsAvailable", key = "#command.userId", condition = "#result.success")
    public RestoreCouponResult restoreCoupon(RestoreCouponCommand command) {
        try {
            // 1. 사용자 쿠폰 조회
            Optional<UserCoupon> userCouponOpt = loadUserCouponPort.loadUserCoupon(command.getUserCouponId());
            if (userCouponOpt.isEmpty()) {
                return RestoreCouponResult.failure("쿠폰을 찾을 수 없습니다.");
            }

            UserCoupon userCoupon = userCouponOpt.get();
            
            // 2. 쿠폰 복원 가능 여부 확인
            if (userCoupon.isAvailable()) {
                return RestoreCouponResult.failure("이미 사용 가능한 쿠폰입니다.");
            }

            // 3. 쿠폰 복원 처리
            userCoupon.restore();
            updateUserCouponPort.updateUserCoupon(userCoupon);

            log.info("쿠폰 복원 성공 - userCouponId: {}, reason: {}", 
                     command.getUserCouponId(), command.getReason());
            
            return RestoreCouponResult.success();
            
        } catch (Exception e) {
            log.error("쿠폰 복원 중 오류 발생 - userCouponId: {}", command.getUserCouponId(), e);
            return RestoreCouponResult.failure("쿠폰 복원 중 오류가 발생했습니다.");
        }
    }

}
