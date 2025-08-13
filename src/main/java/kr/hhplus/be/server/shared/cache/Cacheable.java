package kr.hhplus.be.server.shared.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 캐시 적용을 위한 어노테이션
 * 메서드 결과를 Redis에 캐시하고, 동일한 요청 시 캐시에서 반환
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {
    
    /**
     * 캐시 키 (SpEL 지원)
     * 예: "product:#{#productId}", "user:#{#userId}:coupons"
     */
    String key();
    
    /**
     * 캐시 만료 시간 (초)
     * 기본값: 300초 (5분)
     */
    long expireAfterWrite() default 300L;
    
    /**
     * 캐시 무효화 조건 (SpEL 지원)
     * 조건이 true일 때 캐시하지 않음
     * 예: "#result == null", "#result.isEmpty()"
     */
    String unless() default "";
    
    /**
     * 캐시 적용 조건 (SpEL 지원)
     * 조건이 true일 때만 캐시 적용
     * 예: "#userId > 0"
     */
    String condition() default "";
}
