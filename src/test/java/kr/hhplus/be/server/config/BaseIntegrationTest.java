package kr.hhplus.be.server.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합테스트 베이스 클래스
 * 모든 통합테스트에서 공통으로 사용할 설정들을 포함
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {
    
    // 공통 테스트 설정들이 자동으로 적용됨
    // - TestLogConfiguration (SQL 로깅)
    // - TestcontainersConfiguration (테스트 DB)
    // - 트랜잭션 롤백
    
    /**
     * 테스트 데이터 초기화 (필요시 오버라이드)
     */
    protected void setUpTestData() {
        // 기본 구현은 비어있음
        // 각 테스트 클래스에서 필요시 오버라이드
    }
    
    /**
     * 테스트 데이터 정리 (필요시 오버라이드)
     */
    protected void tearDownTestData() {
        // 기본 구현은 비어있음
        // 각 테스트 클래스에서 필요시 오버라이드
    }
} 