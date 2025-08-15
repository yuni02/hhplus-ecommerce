package kr.hhplus.be.server.shared.config;

import kr.hhplus.be.server.shared.lock.DistributedLockManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * @deprecated Use DistributedLockManager instead
 */
@Slf4j
@Component
@Deprecated
public class RedisDistributedLock {

    private final DistributedLockManager distributedLockManager;

    public RedisDistributedLock(DistributedLockManager distributedLockManager) {
        this.distributedLockManager = distributedLockManager;
    }

    /**
     * 분산락 획득 (레거시 호환성)
     * @param lockKey 락 키
     * @param timeoutSeconds 타임아웃 (초)
     * @return 락 획득 성공 여부
     */
    public boolean tryLock(String lockKey, long timeoutSeconds) {
        log.warn("RedisDistributedLock.tryLock is deprecated. Use DistributedLockManager.executeWithLock instead.");
        return distributedLockManager.isLocked(lockKey);
    }

    /**
     * 분산락 해제 (레거시 호환성)
     * @param lockKey 락 키
     * @return 락 해제 성공 여부
     */
    public boolean releaseLock(String lockKey) {
        log.warn("RedisDistributedLock.releaseLock is deprecated. Use DistributedLockManager.executeWithLock instead.");
        return true;
    }

    /**
     * 락 키 생성 (쿠폰 발급용)
     * @param couponId 쿠폰 ID
     * @return 락 키
     */
    public static String createCouponLockKey(Long couponId) {
        return "coupon:issue:" + couponId;
    }

    /**
     * 락 키 생성 (사용자별 쿠폰 발급용)
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 락 키
     */
    public static String createUserCouponLockKey(Long couponId, Long userId) {
        return "coupon:issue:user:" + couponId + ":" + userId;
    }
}
