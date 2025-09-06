package kr.hhplus.be.server.shared.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 파라미터 기반 락 키 생성기
 * #{parameterName} 형태의 단순한 파라미터 참조를 지원
 * SpEL 대신 정규식을 사용하여 더 안전하고 명확하게 처리
 */
@Slf4j
@Component
public class ParameterBasedLockKeyGenerator implements LockKeyGenerator {
    
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    // #{parameterName} 또는 #{#parameterName} 형태의 패턴 매칭
    private final Pattern PARAMETER_PATTERN = Pattern.compile("#\\{#?([^}]+)\\}");
    
    @Override
    public String generateKey(Method method, Object[] args) {
        // 이 메서드는 supports()에서 true인 경우에만 호출됨
        throw new UnsupportedOperationException("generateKey(Method, Object[]) is not supported. Use generateKey(String, Method, Object[])");
    }
    
    public String generateKey(String keyExpression, Method method, Object[] args) {
        try {
            String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
            if (paramNames == null) {
                log.warn("Parameter names not available for method: {}", method.getName());
                return "lock:" + keyExpression;
            }
            
            String result = keyExpression;
            Matcher matcher = PARAMETER_PATTERN.matcher(keyExpression);
            
            while (matcher.find()) {
                String fullMatch = matcher.group(0); // 전체 매치 #{#paramName} 또는 #{paramName}
                String paramName = matcher.group(1);   // 파라미터 이름
                String replacement = findParameterValue(paramName, paramNames, args);
                result = result.replace(fullMatch, replacement);
            }
            
            return result;
            
        } catch (Exception e) {
            log.warn("Failed to generate parameter-based key: {}, using fallback", keyExpression, e);
            return "lock:" + keyExpression;
        }
    }
    
    @Override
    public boolean supports(String keyExpression) {
        return keyExpression != null && PARAMETER_PATTERN.matcher(keyExpression).find();
    }
    
    /**
     * 파라미터 이름으로 값 찾기
     */
    private String findParameterValue(String paramName, String[] paramNames, Object[] args) {
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            if (paramName.equals(paramNames[i])) {
                return args[i] != null ? args[i].toString() : "null";
            }
        }
        log.warn("Parameter not found: {}", paramName);
        return paramName; // 파라미터를 찾지 못한 경우 원본 이름 반환
    }
}