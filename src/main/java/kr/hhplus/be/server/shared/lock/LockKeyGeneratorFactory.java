package kr.hhplus.be.server.shared.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 락 키 생성기 팩토리
 * 키 표현식에 따라 적절한 생성기를 선택
 */
@Component
@RequiredArgsConstructor
public class LockKeyGeneratorFactory {
    
    private final List<LockKeyGenerator> generators;
    
    /**
     * 키 표현식에 맞는 생성기 반환
     */
    public LockKeyGenerator getGenerator(String keyExpression) {
        return generators.stream()
                .filter(generator -> generator.supports(keyExpression))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No suitable key generator found for: " + keyExpression));
    }
}