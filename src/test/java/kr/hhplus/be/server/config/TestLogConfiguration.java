package kr.hhplus.be.server.config;

import com.p6spy.engine.spy.P6SpyOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * 테스트용 SQL 로깅 설정
 * 모든 통합테스트에서 재사용 가능
 */
@TestConfiguration
@Profile("test") // 테스트 환경에서만 활성화
public class TestLogConfiguration {

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(LogPrettySqlFormatter.class.getName());
        P6SpyOptions.getActiveInstance().setAppender("com.p6spy.engine.spy.appender.Slf4JLogger");
    }
}
