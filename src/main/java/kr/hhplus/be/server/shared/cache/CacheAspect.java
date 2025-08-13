package kr.hhplus.be.server.shared.cache;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

/**
 * 캐시 AOP
 * @Cacheable과 @CacheEvict 어노테이션 처리
 */
@Slf4j
@Aspect
@Component
@Order(2) // 분산 락보다 낮은 우선순위 (분산 락 적용 후 캐시 적용)
public class CacheAspect {

    private final RedisCacheManager cacheManager;
    private final ExpressionParser parser = new SpelExpressionParser();

    public CacheAspect(RedisCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * @Cacheable 어노테이션 처리
     */
    @Around("@annotation(cacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // SpEL 컨텍스트 생성
        EvaluationContext context = createEvaluationContext(method, args, null);

        // 조건 확인
        if (!evaluateCondition(cacheable.condition(), context)) {
            log.debug("Cache condition not met, executing method directly");
            return joinPoint.proceed();
        }

        // 캐시 키 생성
        String cacheKey = evaluateExpression(cacheable.key(), context, String.class);
        if (!StringUtils.hasText(cacheKey)) {
            log.warn("Cache key is empty, executing method directly");
            return joinPoint.proceed();
        }

        // 캐시에서 조회
        Class<?> returnType = method.getReturnType();
        @SuppressWarnings("unchecked")
        Optional<Object> cachedValue = (Optional<Object>) cacheManager.get(cacheKey, Object.class);
        
        if (cachedValue.isPresent()) {
            log.debug("Cache hit - key: {}", cacheKey);
            return cachedValue.get();
        }

        // 캐시 미스 - 메서드 실행
        log.debug("Cache miss - key: {}, executing method", cacheKey);
        Object result = joinPoint.proceed();

        // unless 조건 확인 (결과 포함한 컨텍스트 재생성)
        EvaluationContext resultContext = createEvaluationContext(method, args, result);
        if (!evaluateCondition(cacheable.unless(), resultContext)) {
            // 캐시 저장
            Duration expiration = Duration.ofSeconds(cacheable.expireAfterWrite());
            cacheManager.set(cacheKey, result, expiration);
            log.debug("Cached result - key: {}, expiration: {}", cacheKey, expiration);
        } else {
            log.debug("Unless condition met, not caching result - key: {}", cacheKey);
        }

        return result;
    }

    /**
     * @CacheEvict 어노테이션 처리 (메서드 실행 전)
     */
    @Before("@annotation(cacheEvict) && @annotation(cacheEvict)")
    public void handleCacheEvictBefore(CacheEvict cacheEvict) {
        if (cacheEvict.beforeInvocation()) {
            // TODO: 메서드 시그니처와 인자 정보가 필요함 - Around로 처리하는 것이 좋음
        }
    }

    /**
     * @CacheEvict 어노테이션 처리 (메서드 실행 후)
     */
    @AfterReturning(pointcut = "@annotation(cacheEvict)", returning = "result")
    public void handleCacheEvictAfter(CacheEvict cacheEvict, Object result) {
        if (!cacheEvict.beforeInvocation()) {
            // TODO: 메서드 시그니처와 인자 정보가 필요함 - Around로 처리하는 것이 좋음
        }
    }

    /**
     * @CacheEvict 어노테이션 처리 (Around 방식으로 통합)
     */
    @Around("@annotation(cacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // SpEL 컨텍스트 생성
        EvaluationContext context = createEvaluationContext(method, args, null);

        // beforeInvocation이 true면 메서드 실행 전 캐시 삭제
        if (cacheEvict.beforeInvocation()) {
            evictCache(cacheEvict, context);
        }

        // 메서드 실행
        Object result = joinPoint.proceed();

        // beforeInvocation이 false면 메서드 실행 후 캐시 삭제
        if (!cacheEvict.beforeInvocation()) {
            EvaluationContext resultContext = createEvaluationContext(method, args, result);
            evictCache(cacheEvict, resultContext);
        }

        return result;
    }

    /**
     * 캐시 삭제 실행
     */
    private void evictCache(CacheEvict cacheEvict, EvaluationContext context) {
        // 조건 확인
        if (!evaluateCondition(cacheEvict.condition(), context)) {
            log.debug("Cache evict condition not met, skipping eviction");
            return;
        }

        // 캐시 키 생성
        String cacheKey = evaluateExpression(cacheEvict.key(), context, String.class);
        if (StringUtils.hasText(cacheKey)) {
            cacheManager.delete(cacheKey);
            log.debug("Cache evicted - key: {}", cacheKey);
        } else {
            log.warn("Cache evict key is empty, skipping eviction");
        }
    }

    /**
     * SpEL 평가 컨텍스트 생성
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 메서드 파라미터 바인딩
        String[] paramNames = signature(method).getParameterNames();
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        
        // 결과 바인딩 (있는 경우)
        if (result != null) {
            context.setVariable("result", result);
        }
        
        return context;
    }

    /**
     * SpEL 표현식 평가
     */
    private <T> T evaluateExpression(String expressionString, EvaluationContext context, Class<T> clazz) {
        if (!StringUtils.hasText(expressionString)) {
            return null;
        }
        
        try {
            Expression expression = parser.parseExpression(expressionString);
            return expression.getValue(context, clazz);
        } catch (Exception e) {
            log.error("Failed to evaluate expression: {}", expressionString, e);
            return null;
        }
    }

    /**
     * 조건 표현식 평가 (기본값: true)
     */
    private boolean evaluateCondition(String condition, EvaluationContext context) {
        if (!StringUtils.hasText(condition)) {
            return true;
        }
        
        Boolean result = evaluateExpression(condition, context, Boolean.class);
        return result != null ? result : true;
    }

    /**
     * 메서드 시그니처 헬퍼
     */
    private MethodSignature signature(Method method) {
        // 실제로는 리플렉션을 통해 파라미터 이름을 가져와야 함
        // 여기서는 간단히 arg0, arg1, ... 형태로 처리
        return new MethodSignature() {
            @Override
            public String[] getParameterNames() {
                int paramCount = method.getParameterCount();
                String[] names = new String[paramCount];
                for (int i = 0; i < paramCount; i++) {
                    names[i] = "arg" + i;
                }
                return names;
            }
            
            // 다른 메서드들은 기본 구현
            @Override public String getName() { return method.getName(); }
            @Override public int getModifiers() { return method.getModifiers(); }
            @Override public Class getDeclaringType() { return method.getDeclaringClass(); }
            @Override public String getDeclaringTypeName() { return method.getDeclaringClass().getName(); }
            @Override public Class getReturnType() { return method.getReturnType(); }
            @Override public Method getMethod() { return method; }
            @Override public Class[] getParameterTypes() { return method.getParameterTypes(); }
            @Override public Class[] getExceptionTypes() { return method.getExceptionTypes(); }
            @Override public String toShortString() { return method.toString(); }
            @Override public String toLongString() { return method.toString(); }
        };
    }
}
