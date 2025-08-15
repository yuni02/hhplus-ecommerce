package kr.hhplus.be.server.shared.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockManager {

    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "lock:";
    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * 분산락 획득 및 작업 실행
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        return executeWithLock(lockKey, DEFAULT_LOCK_TIMEOUT, DEFAULT_WAIT_TIMEOUT, task);
    }

    /**
     * 분산락 획득 및 작업 실행 (커스텀 타임아웃)
     */
    public <T> T executeWithLock(String lockKey, Duration lockTimeout, Duration waitTimeout, Supplier<T> task) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        
        try {
            boolean acquired = lock.tryLock(waitTimeout.toMillis(), lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
            
            if (!acquired) {
                throw new RuntimeException("Failed to acquire lock within timeout: " + fullLockKey);
            }
            
            log.debug("Lock acquired - key: {}", fullLockKey);
            return task.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released - key: {}", fullLockKey);
            }
        }
    }

    /**
     * 분산락 획득 및 작업 실행 (void)
     */
    public void executeWithLock(String lockKey, Runnable task) {
        executeWithLock(lockKey, () -> {
            task.run();
            return null;
        });
    }

    /**
     * 분산락 획득 및 작업 실행 (void, 커스텀 타임아웃)
     */
    public void executeWithLock(String lockKey, Duration lockTimeout, Duration waitTimeout, Runnable task) {
        executeWithLock(lockKey, lockTimeout, waitTimeout, () -> {
            task.run();
            return null;
        });
    }

    /**
     * 락 존재 여부 확인
     */
    public boolean isLocked(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        return lock.isLocked();
    }

    /**
     * Fair Lock을 사용한 분산락 획득 및 작업 실행
     * 대기 순서를 보장하고 Pub/Sub을 통한 효율적인 알림 메커니즘 사용
     */
    public <T> T executeWithFairLock(String lockKey, Supplier<T> task) {
        return executeWithFairLock(lockKey, DEFAULT_LOCK_TIMEOUT, DEFAULT_WAIT_TIMEOUT, task);
    }

    /**
     * Fair Lock을 사용한 분산락 획득 및 작업 실행 (커스텀 타임아웃)
     */
    public <T> T executeWithFairLock(String lockKey, Duration lockTimeout, Duration waitTimeout, Supplier<T> task) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock fairLock = redissonClient.getFairLock(fullLockKey);
        
        try {
            boolean acquired = fairLock.tryLock(waitTimeout.toMillis(), lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
            
            if (!acquired) {
                throw new RuntimeException("Failed to acquire fair lock within timeout: " + fullLockKey);
            }
            
            log.debug("Fair lock acquired - key: {}", fullLockKey);
            return task.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Fair lock acquisition interrupted", e);
        } finally {
            if (fairLock.isHeldByCurrentThread()) {
                fairLock.unlock();
                log.debug("Fair lock released - key: {}", fullLockKey);
            }
        }
    }

    /**
     * Fair Lock을 사용한 분산락 획득 및 작업 실행 (void)
     */
    public void executeWithFairLock(String lockKey, Runnable task) {
        executeWithFairLock(lockKey, () -> {
            task.run();
            return null;
        });
    }

    /**
     * Fair Lock을 사용한 분산락 획득 및 작업 실행 (void, 커스텀 타임아웃)
     */
    public void executeWithFairLock(String lockKey, Duration lockTimeout, Duration waitTimeout, Runnable task) {
        executeWithFairLock(lockKey, lockTimeout, waitTimeout, () -> {
            task.run();
            return null;
        });
    }
}
