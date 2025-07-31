package kr.hhplus.be.server.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * 테스트 자동 설정
 * 모든 테스트에서 공통으로 사용할 설정들을 자동으로 포함
 */
@TestConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "kr.hhplus.be.server")
@Import({
    TestLogConfiguration.class,
    // TestcontainersConfiguration.class, // 필요시 추가
    // 다른 공통 테스트 설정들...
})
public class TestAutoConfiguration {
    
    // 추가적인 테스트 설정이 필요한 경우 여기에 작성
} 