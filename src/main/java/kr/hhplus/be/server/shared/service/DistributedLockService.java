package kr.hhplus.be.server.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DistributedLockService {

    private final RedissonClient redissonClient;
    
    private static final long DEFAULT_LOCK_TIMEOUT = 30; // 30초
    private static final long DEFAULT_WAIT_TIMEOUT = 10; // 10초

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    // ==================== 1. Simple Lock ====================
    
    /**
     * 단순 분산락 획득
     */
    public boolean acquireLock(String lockKey, long timeoutSeconds) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(DEFAULT_WAIT_TIMEOUT, timeoutSeconds, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("Simple 락 획득 성공: {}", lockKey);
            } else {
                log.warn("Simple 락 획득 실패 (타임아웃): {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Simple 락 획득 중 인터럽트 발생: {}", lockKey, e);
            return false;
        }
    }

    public boolean acquireLock(String lockKey) {
        return acquireLock(lockKey, DEFAULT_LOCK_TIMEOUT);
    }

    public boolean releaseLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Simple 락 해제 성공: {}", lockKey);
                return true;
            } else {
                log.warn("Simple 락 해제 실패 (소유자가 아님): {}", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("Simple 락 해제 중 오류 발생: {}", lockKey, e);
            return false;
        }
    }

    // ==================== 2. Multi Lock ====================
    
    /**
     * 멀티락 획득 - 여러 락을 원자적으로 획득
     */
    public boolean acquireMultiLock(List<String> lockKeys, long timeoutSeconds) {
        RLock[] locks = lockKeys.stream()
                .map(redissonClient::getLock)
                .toArray(RLock[]::new);
        
        RLock multiLock = redissonClient.getMultiLock(locks);
        
        try {
            boolean acquired = multiLock.tryLock(DEFAULT_WAIT_TIMEOUT, timeoutSeconds, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("Multi 락 획득 성공: {}", lockKeys);
            } else {
                log.warn("Multi 락 획득 실패 (타임아웃): {}", lockKeys);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Multi 락 획득 중 인터럽트 발생: {}", lockKeys, e);
            return false;
        }
    }

    /**
     * 멀티락 해제
     */
    public boolean releaseMultiLock(List<String> lockKeys) {
        RLock[] locks = lockKeys.stream()
                .map(redissonClient::getLock)
                .toArray(RLock[]::new);
        
        RLock multiLock = redissonClient.getMultiLock(locks);
        
        try {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
                log.debug("Multi 락 해제 성공: {}", lockKeys);
                return true;
            } else {
                log.warn("Multi 락 해제 실패 (소유자가 아님): {}", lockKeys);
                return false;
            }
        } catch (Exception e) {
            log.error("Multi 락 해제 중 오류 발생: {}", lockKeys, e);
            return false;
        }
    }

    // ==================== 3. Read-Write Lock ====================
    
    /**
     * 읽기 락 획득
     */
    public boolean acquireReadLock(String lockKey, long timeoutSeconds) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock readLock = readWriteLock.readLock();
        
        try {
            boolean acquired = readLock.tryLock(DEFAULT_WAIT_TIMEOUT, timeoutSeconds, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("Read 락 획득 성공: {}", lockKey);
            } else {
                log.warn("Read 락 획득 실패 (타임아웃): {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Read 락 획득 중 인터럽트 발생: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 쓰기 락 획득
     */
    public boolean acquireWriteLock(String lockKey, long timeoutSeconds) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock writeLock = readWriteLock.writeLock();
        
        try {
            boolean acquired = writeLock.tryLock(DEFAULT_WAIT_TIMEOUT, timeoutSeconds, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("Write 락 획득 성공: {}", lockKey);
            } else {
                log.warn("Write 락 획득 실패 (타임아웃): {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Write 락 획득 중 인터럽트 발생: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 읽기 락 해제
     */
    public boolean releaseReadLock(String lockKey) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock readLock = readWriteLock.readLock();
        
        try {
            if (readLock.isHeldByCurrentThread()) {
                readLock.unlock();
                log.debug("Read 락 해제 성공: {}", lockKey);
                return true;
            } else {
                log.warn("Read 락 해제 실패 (소유자가 아님): {}", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("Read 락 해제 중 오류 발생: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 쓰기 락 해제
     */
    public boolean releaseWriteLock(String lockKey) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock writeLock = readWriteLock.writeLock();
        
        try {
            if (writeLock.isHeldByCurrentThread()) {
                writeLock.unlock();
                log.debug("Write 락 해제 성공: {}", lockKey);
                return true;
            } else {
                log.warn("Write 락 해제 실패 (소유자가 아님): {}", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("Write 락 해제 중 오류 발생: {}", lockKey, e);
            return false;
        }
    }

    // ==================== 4. Fair Lock ====================
    
    /**
     * 공정 락 획득 (FIFO 순서 보장)
     */
    public boolean acquireFairLock(String lockKey, long timeoutSeconds) {
        RLock fairLock = redissonClient.getFairLock(lockKey);
        
        try {
            boolean acquired = fairLock.tryLock(DEFAULT_WAIT_TIMEOUT, timeoutSeconds, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("Fair 락 획득 성공: {}", lockKey);
            } else {
                log.warn("Fair 락 획득 실패 (타임아웃): {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Fair 락 획득 중 인터럽트 발생: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 공정 락 해제
     */
    public boolean releaseFairLock(String lockKey) {
        RLock fairLock = redissonClient.getFairLock(lockKey);
        
        try {
            if (fairLock.isHeldByCurrentThread()) {
                fairLock.unlock();
                log.debug("Fair 락 해제 성공: {}", lockKey);
                return true;
            } else {
                log.warn("Fair 락 해제 실패 (소유자가 아님): {}", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("Fair 락 해제 중 오류 발생: {}", lockKey, e);
            return false;
        }
    }

    // ==================== 5. 락 상태 확인 ====================
    
    /**
     * 락 보유 여부 확인
     */
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * 현재 스레드가 락을 보유하고 있는지 확인
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 락 대기 중인 스레드 수 확인
     */
    public int getHoldCount(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.getHoldCount();
    }

    // ==================== 6. 락 키 생성기 ====================
    
    public static class LockKeyGenerator {
        public static String orderLock(Long userId) {
            return String.format("order:user:%d", userId);
        }

        public static String balanceLock(Long userId) {
            return String.format("balance:user:%d", userId);
        }

        public static String couponIssueLock(Long couponId) {
            return String.format("coupon:issue:%d", couponId);
        }

        public static String couponUseLock(Long userCouponId) {
            return String.format("coupon:use:%d", userCouponId);
        }

        public static String productStockLock(Long productId) {
            return String.format("product:stock:%d", productId);
        }
        
        public static String multiLock(List<String> keys) {
            return String.format("multi:%s", String.join(":", keys));
        }
        
        public static String readWriteLock(String resource) {
            return String.format("rw:%s", resource);
        }
        
        public static String fairLock(String resource) {
            return String.format("fair:%s", resource);
        }
    }
}
