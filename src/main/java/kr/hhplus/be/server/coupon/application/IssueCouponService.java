package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadUserPort;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import kr.hhplus.be.server.shared.lock.DistributedLock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

/**
 * 쿠폰 발급 Application 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueCouponService implements IssueCouponUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadCouponPort loadCouponPort;
    private final SaveUserCouponPort saveUserCouponPort;
    private final RedisCouponService redisCouponService;    // 선착순 체크를 위한 Redis 서비스  

    @Override   
    @DistributedLock(
        key = "'coupon-issue:' + #command.couponId",
        waitTime = 10,
        leaseTime = 15,
        timeUnit = TimeUnit.SECONDS,
        fair = true
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public IssueCouponResult issueCoupon(IssueCouponCommand command) {
        return processCouponIssue(command);
    }
    
    private IssueCouponResult processCouponIssue(IssueCouponCommand command) {
        try {
            // 1. 입력값 검증
            if (command.getUserId() == null || command.getUserId() <= 0) {
                return IssueCouponResult.failure("잘못된 사용자 ID입니다.");
            }
            
            // 2. 사용자 존재 확인
            if (!loadUserPort.existsById(command.getUserId())) {
                return IssueCouponResult.failure("사용자를 찾을 수 없습니다.");
            }

            // 3. Redis에서 쿠폰 정보 조회 (빠른 처리)
            Optional<RedisCouponService.CouponInfo> cachedCouponInfo = 
                redisCouponService.getCouponInfoFromCache(command.getCouponId());
            
            LoadCouponPort.CouponInfo couponInfo;
            
            if (cachedCouponInfo.isPresent()) {
                // Redis에서 쿠폰 정보를 찾은 경우
                RedisCouponService.CouponInfo cached = cachedCouponInfo.get();
                couponInfo = new LoadCouponPort.CouponInfo(
                    cached.getId(), cached.getName(), cached.getDescription(),
                    cached.getDiscountAmount(), cached.getMaxIssuanceCount(),
                    cached.getIssuedCount(), cached.getStatus(),
                    cached.getValidFrom(), cached.getValidTo()
                );
            } else {
                // Redis에 없으면 DB에서 조회하고 캐싱
                couponInfo = loadCouponPort.loadCouponByIdWithLock(command.getCouponId())
                        .orElse(null);
                
                if (couponInfo == null) {
                    return IssueCouponResult.failure("존재하지 않는 쿠폰입니다.");
                }
                
                // DB에서 조회한 정보를 Redis에 캐싱
                redisCouponService.cacheCouponInfo(
                    couponInfo.getId(), couponInfo.getName(), couponInfo.getDescription(),
                    couponInfo.getDiscountAmount(), couponInfo.getMaxIssuanceCount(),
                    couponInfo.getIssuedCount(), couponInfo.getStatus(),
                    couponInfo.getValidFrom(), couponInfo.getValidTo()
                );
            }

            // 4. 쿠폰 발급 가능 여부 확인
            if (!canIssueCoupon(couponInfo)) {
                return IssueCouponResult.failure("발급할 수 없는 쿠폰입니다.");
            }

            // 5. Redis 기반 선착순 체크 (빠른 실패)
            RedisCouponService.CouponIssueResult redisResult = 
                redisCouponService.checkAndIssueCoupon(command.getCouponId(), command.getUserId(), couponInfo.getMaxIssuanceCount());
            
            if (!redisResult.isSuccess() && !redisResult.shouldFallbackToDb()) {
                return IssueCouponResult.failure(redisResult.getErrorMessage());
            }

            // 6. DB 기반 쿠폰 발급 수량을 원자적으로 증가 (Redis 실패 시 또는 이중 검증)
            if (!loadCouponPort.incrementIssuedCount(command.getCouponId())) {
                return IssueCouponResult.failure("쿠폰이 모두 소진되었습니다. 선착순 발급에 실패했습니다.");
            }

            // 7. Redis 캐시 업데이트 (발급 수량 증가)
            redisCouponService.updateCouponIssuedCount(command.getCouponId(), couponInfo.getIssuedCount() + 1);

            // 8. 사용자 쿠폰 생성
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
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 상태 체크
        if (!"ACTIVE".equals(couponInfo.getStatus())) {
            return false;
        }
        
        // 2. 발급 수량 체크
        if (couponInfo.getIssuedCount() >= couponInfo.getMaxIssuanceCount()) {
            return false;
        }
        
        // 3. 발급 시작일 체크 (validFrom이 설정되어 있고, 아직 시작되지 않은 경우)
        if (couponInfo.getValidFrom() != null && now.isBefore(couponInfo.getValidFrom())) {
            return false;
        }
        
        // 4. 발급 종료일 체크 (validTo가 설정되어 있고, 이미 만료된 경우)
        if (couponInfo.getValidTo() != null && now.isAfter(couponInfo.getValidTo())) {
            return false;
        }
        
        return true;
    }
} 