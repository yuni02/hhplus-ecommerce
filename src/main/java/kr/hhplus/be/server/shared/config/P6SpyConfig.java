package kr.hhplus.be.server.shared.config;

import com.p6spy.engine.spy.P6SpyOptions;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class P6SpyConfig {

    @PostConstruct
    public void setLogMessageFormat() {
        // P6Spy 로그 포맷터 설정 (예쁜 SQL 포맷팅)
        P6SpyOptions.getActiveInstance().setLogMessageFormat("com.p6spy.engine.spy.appender.LogPrettySqlFormatter");
        
        // 로그 출력 방식 설정 (SLF4J 사용)
        P6SpyOptions.getActiveInstance().setAppender("com.p6spy.engine.spy.appender.Slf4JLogger");
    }
} 