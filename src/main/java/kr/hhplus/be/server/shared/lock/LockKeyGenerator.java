package kr.hhplus.be.server.shared.lock;

import java.lang.reflect.Method;

/**
 * 분산 락 키 생성 인터페이스
 * SpEL 표현식 대신 타입 안전한 키 생성을 제공
 */
public interface LockKeyGenerator {
    
    /**
     * 락 키 생성
     * 
     * @param method 대상 메서드
     * @param args 메서드 파라미터
     * @return 생성된 락 키
     */
    String generateKey(Method method, Object[] args);
    
    /**
     * 키 생성 타입 확인
     * 
     * @param keyExpression 키 표현식
     * @return 이 생성기가 처리할 수 있는지 여부
     */
    boolean supports(String keyExpression);
}