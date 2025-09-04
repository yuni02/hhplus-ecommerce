package kr.hhplus.be.server.shared.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * SpEL 표현식 기반 락 키 생성기
 * 'order_user_' + #command.userId 같은 SpEL 표현식을 지원
 */
@Slf4j
@Component
public class SpelLockKeyGenerator implements LockKeyGenerator {
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    
    @Override
    public String generateKey(Method method, Object[] args) {
        throw new UnsupportedOperationException("generateKey(Method, Object[]) is not supported. Use generateKey(String, Method, Object[])");
    }
    
    public String generateKey(String keyExpression, Method method, Object[] args) {
        try {
            String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
            if (paramNames == null) {
                log.warn("Parameter names not available for method: {}", method.getName());
                return "lock:" + keyExpression;
            }
            
            StandardEvaluationContext context = new StandardEvaluationContext();
            
            // 메서드 파라미터들을 SpEL 컨텍스트에 추가
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            
            Expression expression = parser.parseExpression(keyExpression);
            Object result = expression.getValue(context);
            String lockKey = result != null ? result.toString() : "null";
            
            log.debug("Generated SpEL lock key: {} from expression: {}", lockKey, keyExpression);
            return lockKey;
            
        } catch (Exception e) {
            log.warn("Failed to generate SpEL-based key: {}, using fallback", keyExpression, e);
            return "lock:" + keyExpression;
        }
    }
    
    @Override
    public boolean supports(String keyExpression) {
        // SpEL 표현식 패턴 감지: 문자열 리터럴 + #변수 조합
        return keyExpression != null && 
               (keyExpression.contains("'") && keyExpression.contains("#")) ||
               (keyExpression.contains("\"") && keyExpression.contains("#")) ||
               keyExpression.matches(".*['\"].*\\+.*#.*");
    }
}