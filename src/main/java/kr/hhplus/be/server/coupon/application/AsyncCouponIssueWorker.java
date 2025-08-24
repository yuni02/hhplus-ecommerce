package kr.hhplus.be.server.coupon.application;

import kr.hhplus.be.server.coupon.application.port.in.IssueCouponUseCase;
import kr.hhplus.be.server.coupon.application.port.out.LoadCouponPort;
import kr.hhplus.be.server.coupon.application.port.out.SaveUserCouponPort;
import kr.hhplus.be.server.coupon.domain.UserCoupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 비동기 쿠폰 발급 워커
 * Redis 대기열에서 순차적으로 쿠폰 발급 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncCouponIssueWorker {

    private final RedisCouponQueueService queueService;  // 대기열 서비스
    private final IssueCouponUseCase issueCouponUseCase;  // 쿠폰 발급 서비스
    private final RedisCouponService redisCouponService;  // 선착순 체크를 위한 Redis 서비스
    private final LoadCouponPort loadCouponPort;  // 쿠폰 정보 조회
    private final SaveUserCouponPort saveUserCouponPort;  // 사용자 쿠폰 저장
    
    /**
     * 비동기 쿠폰 발급 처리 (Redis 최적화 버전)
     */
    @Async("couponIssueExecutor")
    public CompletableFuture<Void> processCouponIssue(Long couponId, Long userId) {
        log.debug("쿠폰 발급 처리 시작 - couponId: {}, userId: {}", couponId, userId);
        
        try {
            // 1. 쿠폰 정보 조회
            var couponInfoOpt = loadCouponPort.loadCouponById(couponId);
            if (couponInfoOpt.isEmpty()) {
                queueService.saveIssueResult(couponId, userId, false, "존재하지 않는 쿠폰입니다.");
                queueService.removeFromQueue(couponId, userId);
                return CompletableFuture.completedFuture(null);
            }
            
            LoadCouponPort.CouponInfo couponInfo = couponInfoOpt.get();
            Integer maxIssuanceCount = couponInfo.getMaxIssuanceCount();
            
            // 2. Redis 기반 빠른 체크 (최적화된 방법 사용)
            RedisCouponService.CouponIssueResult redisResult = 
                redisCouponService.checkAndIssueCouponOptimized(couponId, userId, maxIssuanceCount);   
            
            if (redisResult.isSuccess()) {
                // Redis에서 성공한 경우 직접 DB에 사용자 쿠폰 생성 (발급 수량 체크 우회)
                try {
                    log.info("DB 사용자 쿠폰 생성 시도 - couponId: {}, userId: {}", couponId, userId);
                    
                    // DB에서 직접 사용자 쿠폰 생성 (쿠폰 발급 수량은 Redis에서 이미 관리됨)
                    UserCoupon userCoupon = UserCoupon.builder()
                            .userId(userId)
                            .couponId(couponId)
                            .discountAmount(couponInfo.getDiscountAmount())
                            .issuedAt(LocalDateTime.now())
                            .build();
                    
                    // 사용자 쿠폰 저장
                    saveUserCouponPort.saveUserCoupon(userCoupon);
                    
                    // 쿠폰 발급 수량도 DB에 반영 (Redis와 동기화)
                    loadCouponPort.incrementIssuedCount(couponId);
                    
                    log.info("DB 사용자 쿠폰 생성 성공 - couponId: {}, userId: {}", couponId, userId);
                } catch (Exception dbException) {
                    log.error("DB 사용자 쿠폰 생성 실패 (Redis는 성공) - couponId: {}, userId: {}", couponId, userId, dbException);
                    // Redis는 성공했으므로 결과는 성공으로 처리
                }
                
                queueService.saveIssueResult(couponId, userId, true, "쿠폰 발급 성공");
                log.info("쿠폰 발급 성공 (Redis) - couponId: {}, userId: {}", couponId, userId);
                
            } else if (redisResult.shouldFallbackToDb()) {
                // Redis 실패 시 DB로 fallback
                try {
                    IssueCouponUseCase.IssueCouponCommand command = 
                        new IssueCouponUseCase.IssueCouponCommand(userId, couponId);
                    IssueCouponUseCase.IssueCouponResult result = issueCouponUseCase.issueCoupon(command);
                    
                    if (result.isSuccess()) {
                        queueService.saveIssueResult(couponId, userId, true, "쿠폰 발급 성공 (DB)");
                        log.info("쿠폰 발급 성공 (DB fallback) - couponId: {}, userId: {}", couponId, userId);
                    } else {
                        queueService.saveIssueResult(couponId, userId, false, result.getErrorMessage());
                        log.warn("쿠폰 발급 실패 (DB fallback) - couponId: {}, userId: {}, reason: {}", 
                            couponId, userId, result.getErrorMessage());
                    }
                } catch (Exception dbException) {
                    queueService.saveIssueResult(couponId, userId, false, "DB 처리 중 오류가 발생했습니다: " + dbException.getMessage());
                    log.error("DB 쿠폰 발급 처리 중 오류 발생 - couponId: {}, userId: {}", couponId, userId, dbException);
                }
            } else {
                // Redis에서 실패 (이미 발급됨, 수량 소진 등)
                queueService.saveIssueResult(couponId, userId, false, redisResult.getErrorMessage());
                log.warn("쿠폰 발급 실패 (Redis) - couponId: {}, userId: {}, reason: {}", 
                    couponId, userId, redisResult.getErrorMessage());
            }
            
            // 3. 대기열에서 제거 (이미 getNextUserFromQueue에서 제거됨)
            
        } catch (Exception e) {
            log.error("쿠폰 발급 처리 중 오류 발생 - couponId: {}, userId: {}", couponId, userId, e);
            queueService.saveIssueResult(couponId, userId, false, "처리 중 오류가 발생했습니다: " + e.getMessage());
            // 대기열에서 제거는 이미 getNextUserFromQueue에서 처리됨
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 대기열 배치 처리 (스케줄러에서 호출)
     * 한 번에 여러 사용자를 처리하여 성능 향상
     */
    public void processQueueBatch(Long couponId, int batchSize) {
        log.debug("쿠폰 대기열 배치 처리 시작 - couponId: {}, batchSize: {}", couponId, batchSize);
        
        try {
            // 배치 크기만큼 사용자들을 대기열에서 가져와서 처리
            for (int i = 0; i < batchSize; i++) {
                Long userId = queueService.getNextUserFromQueue(couponId);
                
                if (userId != null) {
                    // 비동기로 쿠폰 발급 처리
                    processCouponIssue(couponId, userId);
                } else {
                    // 더 이상 대기열에 사용자가 없으면 종료
                    break;
                }
            }
            
        } catch (Exception e) {
            log.error("쿠폰 대기열 배치 처리 중 오류 발생 - couponId: {}", couponId, e);
        }
    }

    /**
     * 대기열 폴링 및 처리 (스케줄러에서 호출) - 기존 메서드 유지
     */
    public void processQueue(Long couponId) {
        log.debug("쿠폰 대기열 처리 시작 - couponId: {}", couponId);
        
        try {
            // 대기열에서 다음 사용자 조회
            Long userId = queueService.getNextUserFromQueue(couponId);
            
            if (userId != null) {
                // 비동기로 쿠폰 발급 처리
                processCouponIssue(couponId, userId);
            }
            
        } catch (Exception e) {
            log.error("쿠폰 대기열 처리 중 오류 발생 - couponId: {}", couponId, e);
        }
    }
}
