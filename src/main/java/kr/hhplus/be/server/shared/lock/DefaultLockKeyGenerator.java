package kr.hhplus.be.server.shared.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 기본 락 키 생성기
 * 클래스명.메서드명.파라미터값 형태로 키 생성
 */
@Slf4j
@Component
public class DefaultLockKeyGenerator implements LockKeyGenerator {
    
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    
    @Override
    public String generateKey(Method method, Object[] args) {
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String params = generateParamString(method, args);
        
        return String.format("lock:%s.%s:%s", className, methodName, params);
    }
    
    @Override
    public boolean supports(String keyExpression) {
        // 빈 문자열이면 기본 키 생성기 사용
        return keyExpression == null || keyExpression.isEmpty();
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