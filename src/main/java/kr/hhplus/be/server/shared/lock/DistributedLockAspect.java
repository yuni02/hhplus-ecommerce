package kr.hhplus.be.server.shared.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

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
        
        // 1. 어노테이션에 key가 지정된 경우 SpEL로 평가
        if (!distributedLock.key().isEmpty()) {
            return evaluateSpEL(distributedLock.key(), method, joinPoint.getArgs());
        }
        
        // 2. 기본 키 생성: 클래스명.메서드명.파라미터값들
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String params = generateParamString(method, joinPoint.getArgs());
        
        return String.format("lock:%s.%s:%s", className, methodName, params);
    }

    /**
     * SpEL 표현식 평가
     */
    private String evaluateSpEL(String expression, Method method, Object[] args) {
        try {
            Expression exp = expressionParser.parseExpression(expression);
            EvaluationContext context = new StandardEvaluationContext();
            
            // 파라미터 이름과 값을 컨텍스트에 추가
            String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            
            Object result = exp.getValue(context);
            return result != null ? result.toString() : expression;
            
        } catch (Exception e) {
            log.warn("Failed to evaluate SpEL expression: {}, using fallback", expression, e);
            return "lock:" + expression;
        }
    }

    /**
     * 파라미터 문자열 생성
     */
    private String generateParamString(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return "no-params";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append("-");
            sb.append(args[i] != null ? args[i].toString() : "null");
        }
        return sb.toString();
    }
}
