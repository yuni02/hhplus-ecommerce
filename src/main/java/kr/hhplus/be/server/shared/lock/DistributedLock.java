package kr.hhplus.be.server.shared.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Redisson을 사용한 분산락 어노테이션
 * AOP 방식으로 메서드 실행 시 자동으로 분산락을 획득하고 해제합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    
    /**
     * 락 키를 생성하는 SpEL 표현식
     * 기본값: 메서드명 + 파라미터 값들
     */
    String key() default "";
    
    /**
     * 락 대기 시간 (기본값: 3초)
     */
    long waitTime() default 3;
    
    /**
     * 락 보유 시간 (기본값: 10초)
     */
    long leaseTime() default 10;
    
    /**
     * 시간 단위 (기본값: 초)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 락 획득 실패 시 예외 발생 여부 (기본값: true)
     */
    boolean throwException() default true;
    
    /**
     * Fair Lock 사용 여부 (기본값: false)
     * true인 경우 대기 순서를 보장하고 Pub/Sub 기반 효율적인 알림 사용
     */
    boolean fair() default false;
}
