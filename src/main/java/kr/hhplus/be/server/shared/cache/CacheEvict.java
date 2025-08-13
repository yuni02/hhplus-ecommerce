package kr.hhplus.be.server.shared.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 캐시 무효화를 위한 어노테이션
 * 메서드 실행 후 지정된 캐시를 삭제
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {
    
    /**
     * 삭제할 캐시 키 (SpEL 지원)
     * 예: "product:#{#productId}", "user:#{#userId}:coupons"
     */
    String key();
    
    /**
     * 메서드 실행 전에 캐시 삭제 여부
     * true: 메서드 실행 전 삭제
     * false: 메서드 실행 후 삭제 (기본값)
     */
    boolean beforeInvocation() default false;
    
    /**
     * 캐시 삭제 조건 (SpEL 지원)
     * 조건이 true일 때만 캐시 삭제
     * 예: "#result.success"
     */
    String condition() default "";
}
