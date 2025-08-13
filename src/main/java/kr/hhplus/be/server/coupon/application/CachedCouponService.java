package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.GetUserCouponsUseCase;
import kr.hhplus.be.server.coupon.application.port.in.UseCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.UpdateUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.shared.cache.CacheConstants;
import kr.hhplus.be.server.shared.cache.CacheKeyGenerator;
import kr.hhplus.be.server.shared.cache.RedisCacheManager;
import kr.hhplus.be.server.shared.cache.Cacheable;
import kr.hhplus.be.server.shared.cache.CacheEvict;
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
    private final RedisCacheManager cacheManager;
    private final CacheKeyGenerator keyGenerator;

    public CachedCouponService(LoadCouponPort loadCouponPort,
                             LoadUserCouponPort loadUserCouponPort,
                             UpdateUserCouponPort updateUserCouponPort,
                             RedisCacheManager cacheManager,
                             CacheKeyGenerator keyGenerator) {
        this.loadCouponPort = loadCouponPort;
        this.loadUserCouponPort = loadUserCouponPort;
        this.updateUserCouponPort = updateUserCouponPort;
        this.cacheManager = cacheManager;
        this.keyGenerator = keyGenerator;
    }

    /**
     * 사용 가능한 쿠폰 목록 조회 (캐시 적용) - 자주 조회됨
     */
    @Cacheable(
        key = "'user-coupons-available:' + #userId",
        expireAfterWrite = 180L, // 3분간 캐시
        unless = "#result.isEmpty()"
    )
    public List<LoadUserCouponPort.UserCouponInfo> getAvailableUserCoupons(Long userId) {
        log.debug("사용 가능한 쿠폰 목록 조회 (DB) - userId: {}", userId);
        
        List<LoadUserCouponPort.UserCouponInfo> availableCoupons = loadUserCouponPort.loadUserCouponsByUserId(userId)
            .stream()
            .filter(coupon -> "AVAILABLE".equals(coupon.getStatus()))
            .toList();
        
        log.debug("Available user coupons retrieved - userId: {}, count: {}", userId, availableCoupons.size());
        return availableCoupons;
    }

    /**
     * 사용 가능한 쿠폰 캐시 무효화
     */
    @CacheEvict(key = "'user-coupons-available:' + #userId")
    public void evictAvailableUserCouponsCache(Long userId) {
        log.debug("사용 가능한 쿠폰 캐시 무효화 - userId: {}", userId);
        // 메서드 내용 없음 - AOP가 캐시 무효화 처리
    }

    /**
     * 전체 사용자 쿠폰 캐시 무효화
     */
    @CacheEvict(key = "'user-coupons-all:' + #userId")
    public void evictAllUserCouponsCache(Long userId) {
        log.debug("전체 사용자 쿠폰 캐시 무효화 - userId: {}", userId);
        // 메서드 내용 없음 - AOP가 캐시 무효화 처리
    }

    /**
     * 사용자 쿠폰 목록 조회 (전체, 캐시 적용) - API 응답용
     */
    @Cacheable(
        key = "'user-coupons-all:' + #userId",
        expireAfterWrite = 300L, // 5분간 캐시
        unless = "#result.isEmpty()"
    )
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
                .collect(Collectors.toList());
        
        return new GetUserCouponsResult(userCoupons);
    }

    @Override
    @CacheEvict(
        key = "'user-coupons-available:' + #command.userId",
        condition = "#result.success"
    )
    public UseCouponResult useCoupon(UseCouponCommand command) {
        try {
            // 1. 사용자 쿠폰 조회
            Optional<UserCoupon> userCouponOpt = loadUserCouponPort.loadUserCoupon(command.getUserCouponId());
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
            log.error("쿠폰 사용 중 오류 발생 - userCouponId: {}", command.getUserCouponId(), e);
            return UseCouponResult.failure("쿠폰 사용 중 오류가 발생했습니다.");
        }
    }

    @Override
    @CacheEvict(
        key = "'user-coupons-available:' + #command.userId",
        condition = "#result.success"
    )
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

    /**
     * 쿠폰 정보 캐시 무효화 (쿠폰 정보 변경 시에만 호출)
     */
    @CacheEvict(key = "'coupon-info:' + #couponId")
    public void evictCouponInfoCache(Long couponId) {
        log.debug("쿠폰 정보 캐시 무효화 - couponId: {}", couponId);
        // 메서드 내용 없음 - AOP가 캐시 무효화 처리
    }
}
