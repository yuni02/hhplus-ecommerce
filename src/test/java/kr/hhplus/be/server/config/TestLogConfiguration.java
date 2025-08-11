package kr.hhplus.be.server.config;

import com.p6spy.engine.spy.P6SpyOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class TestLogConfiguration {

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(LogPrettySqlFormatter.class.getName());
        P6SpyOptions.getActiveInstance().setAppender("com.p6spy.engine.spy.appender.Slf4JLogger");
    }
}
