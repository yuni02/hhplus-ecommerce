package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.config.TestLogConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개별 테스트에서 TestLogConfiguration만 필요한 경우의 예시
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestLogConfiguration.class) // 개별적으로 import
class IndividualTestExample {

    @Test
    void 개별_테스트_예시() {
        // 이 테스트에서만 SQL 로깅이 활성화됨
        // TestLogConfiguration이 자동으로 적용됨
    }
} 