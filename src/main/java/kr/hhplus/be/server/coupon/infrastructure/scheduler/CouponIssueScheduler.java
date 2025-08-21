package kr.hhplus.be.server.coupon.infrastructure.scheduler;

import kr.hhplus.be.server.coupon.application.AsyncCouponIssueWorker;
import kr.hhplus.be.server.coupon.application.RedisCouponQueueService;
import kr.hhplus.be.server.coupon.application.port.in.GetActiveCouponsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 쿠폰 발급 스케줄러
 * Redis 대기열을 주기적으로 폴링하여 쿠폰 발급 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueScheduler {

    private final AsyncCouponIssueWorker asyncCouponIssueWorker;
    private final RedisCouponQueueService queueService;
    private final GetActiveCouponsUseCase getActiveCouponsUseCase;

    /**
     * 쿠폰 발급 대기열 처리 (100ms마다 실행 - 더 빠른 응답)
     */
    @Scheduled(fixedRate = 100)
    public void processCouponQueues() {
        try {
            // 활성 쿠폰 목록을 조회하여 각각의 대기열 처리
            Set<Long> activeCouponIds = getActiveCouponIds();
            log.info("스케줄러 실행 - 활성 쿠폰 수: {}", activeCouponIds.size());
            
            for (Long couponId : activeCouponIds) {
                // 대기열이 있는 경우에만 처리
                Long queueSize = queueService.getQueueSize(couponId);
                log.info("쿠폰 {} - 대기열 크기: {}", couponId, queueSize);
                if (queueSize != null && queueSize > 0) {
                    // 한 번에 여러 사용자 처리 (최대 10명씩)
                    int batchSize = Math.min(10, queueSize.intValue());
                    log.info("쿠폰 {} - 배치 처리 시작, 배치 크기: {}", couponId, batchSize);
                    asyncCouponIssueWorker.processQueueBatch(couponId, batchSize);
                }
            }
            
        } catch (Exception e) {
            log.error("쿠폰 발급 스케줄러 실행 중 오류 발생", e);
        }
    }

    /**
     * 활성 쿠폰 ID 목록 조회
     */
    private Set<Long> getActiveCouponIds() {
        try {
            // DB에서 활성 쿠폰 조회
            GetActiveCouponsUseCase.GetActiveCouponsCommand command = 
                new GetActiveCouponsUseCase.GetActiveCouponsCommand();
            
            GetActiveCouponsUseCase.GetActiveCouponsResult result = 
                getActiveCouponsUseCase.getActiveCoupons(command);
            
            return result.getActiveCoupons().stream()
                .map(GetActiveCouponsUseCase.ActiveCouponInfo::getCouponId)
                .collect(Collectors.toSet());
                
        } catch (Exception e) {
            log.warn("활성 쿠폰 조회 실패, 빈 셋 반환", e);
            return Set.of();
        }
    }
}
