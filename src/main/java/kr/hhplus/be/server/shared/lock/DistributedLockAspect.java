package kr.hhplus.be.server.shared.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(1) // 트랜잭션보다 높은 우선순위 (락을 먼저 획득)
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final LockKeyGeneratorFactory keyGeneratorFactory;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = generateLockKey(joinPoint, distributedLock);
        RLock lock = distributedLock.fair() ? 
            redissonClient.getFairLock(lockKey) : 
            redissonClient.getLock(lockKey);
        
        boolean acquired = false;
        try {
            // 락 획득 시도
            acquired = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            
            if (!acquired) {
                log.warn("Failed to acquire distributed lock - key: {}, method: {}", 
                    lockKey, joinPoint.getSignature().getName());
                
                if (distributedLock.throwException()) {
                    throw new RuntimeException("Failed to acquire distributed lock: " + lockKey);
                }
                return null;
            }
            
            log.debug("{} lock acquired - key: {}, method: {}", 
                distributedLock.fair() ? "Fair" : "Distributed", lockKey, joinPoint.getSignature().getName());
            
            // 원본 메서드 실행
            return joinPoint.proceed();
            
        } finally {
            // 락 해제
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("{} lock released - key: {}, method: {}", 
                    distributedLock.fair() ? "Fair" : "Distributed", lockKey, joinPoint.getSignature().getName());
            }
        }
    }

    /**
     * 락 키 생성
     */
    private String generateLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String keyExpression = distributedLock.key();
        
        LockKeyGenerator generator = keyGeneratorFactory.getGenerator(keyExpression);
        
        // 파라미터 기반 생성기인 경우
        if (generator instanceof ParameterBasedLockKeyGenerator) {
            return ((ParameterBasedLockKeyGenerator) generator).generateKey(keyExpression, method, joinPoint.getArgs());
        }
        
        // 기본 생성기인 경우
        return generator.generateKey(method, joinPoint.getArgs());
    }

}
