package kr.hhplus.be.server.integration.cache;

import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.shared.cache.demo.CacheDemo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AOP 기반 캐시 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("AOP 캐시 통합 테스트")
class AopCacheIntegrationTest {

    @Autowired
    private CacheDemo cacheDemo;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("@Cacheable 어노테이션 캐시 저장 및 조회 테스트")
    void cacheableTest() throws InterruptedException {
        String userId = "1";
        String cacheKey = "user-name:" + userId;

        // 첫 번째 호출 - DB에서 조회 (캐시 저장)
        long startTime1 = System.currentTimeMillis();
        String result1 = cacheDemo.getUserName(userId);
        long endTime1 = System.currentTimeMillis();
        
        assertThat(result1).isEqualTo("홍길동");
        assertThat(endTime1 - startTime1).isGreaterThan(900); // 1초 지연 포함
        
        // Redis에 캐시 저장 확인
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // 두 번째 호출 - 캐시에서 조회 (빠른 응답)
        long startTime2 = System.currentTimeMillis();
        String result2 = cacheDemo.getUserName(userId);
        long endTime2 = System.currentTimeMillis();
        
        assertThat(result2).isEqualTo("홍길동");
        assertThat(endTime2 - startTime2).isLessThan(100); // 빠른 응답
        
        // 결과 일치 확인
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("@CacheEvict 어노테이션 캐시 무효화 테스트")
    void cacheEvictTest() throws InterruptedException {
        String userId = "2";
        String cacheKey = "user-name:" + userId;

        // 첫 번째 호출 - 캐시 저장
        String result1 = cacheDemo.getUserName(userId);
        assertThat(result1).isEqualTo("김철수");
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // 사용자 이름 업데이트 - 캐시 무효화
        boolean updated = cacheDemo.updateUserName(userId, "김영수");
        assertThat(updated).isTrue();
        
        // 캐시 무효화 확인
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();

        // 다시 호출 - DB에서 새로운 데이터 조회
        long startTime = System.currentTimeMillis();
        String result2 = cacheDemo.getUserName(userId);
        long endTime = System.currentTimeMillis();
        
        assertThat(result2).isEqualTo("김영수"); // 업데이트된 이름
        assertThat(endTime - startTime).isGreaterThan(900); // DB 조회 지연시간 포함
    }

    @Test
    @DisplayName("캐시 조건부 적용 테스트")
    void conditionalCacheTest() {
        String cacheKey1 = "user-data:valid";
        String cacheKey2 = "user-data:";

        // 유효한 userId - 캐시 적용
        String result1 = cacheDemo.getUserData("valid");
        assertThat(result1).isEqualTo("Data for user valid");
        assertThat(redisTemplate.hasKey(cacheKey1)).isTrue();

        // 빈 userId - 캐시 적용 안됨 (condition)
        String result2 = cacheDemo.getUserData("");
        assertThat(result2).isNull();
        assertThat(redisTemplate.hasKey(cacheKey2)).isFalse();

        // null userId - 캐시 적용 안됨 (condition)
        String result3 = cacheDemo.getUserData(null);
        assertThat(result3).isNull();
    }

    @Test
    @DisplayName("수동 캐시 무효화 테스트")
    void manualCacheEvictTest() {
        String userId = "3";
        String cacheKey = "user-name:" + userId;

        // 캐시 저장
        String result1 = cacheDemo.getUserName(userId);
        assertThat(result1).isEqualTo("이영희");
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // 수동 캐시 무효화
        cacheDemo.evictUserCache(userId);
        
        // 캐시 무효화 확인
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();
    }

    @Test
    @DisplayName("캐시 만료시간 확인 테스트")
    void cacheExpirationTest() throws InterruptedException {
        String userId = "1";
        String cacheKey = "user-name:" + userId;

        // 캐시 저장
        cacheDemo.getUserName(userId);
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();
        
        // TTL 확인 (300초 = 5분)
        Long ttl = redisTemplate.getExpire(cacheKey);
        assertThat(ttl).isGreaterThan(250L).isLessThanOrEqualTo(300L);
    }
}
