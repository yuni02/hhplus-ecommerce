package kr.hhplus.be.server.shared.cache.demo;

import kr.hhplus.be.server.shared.cache.Cacheable;
import kr.hhplus.be.server.shared.cache.CacheEvict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AOP 캐시 데모 서비스
 * @Cacheable과 @CacheEvict 어노테이션 사용 예제
 */
@Slf4j
@Service
public class CacheDemo {

    // 가상의 데이터베이스 역할
    private final Map<String, String> fakeDatabase = new HashMap<>();

    public CacheDemo() {
        // 초기 데이터 설정
        fakeDatabase.put("user:1", "홍길동");
        fakeDatabase.put("user:2", "김철수");
        fakeDatabase.put("user:3", "이영희");
    }

    /**
     * 사용자 이름 조회 - 캐시 적용
     * 5분간 캐시 유지
     */
    @Cacheable(
        key = "'user-name:' + #arg0",
        expireAfterWrite = 300L,
        unless = "#result == null"
    )
    public String getUserName(String userId) {
        log.info("데이터베이스에서 사용자 이름 조회 - userId: {}", userId);
        
        // 가상의 DB 조회 지연시간
        try {
            Thread.sleep(1000); // 1초 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return fakeDatabase.get("user:" + userId);
    }

    /**
     * 사용자 정보 업데이트 - 관련 캐시 무효화
     */
    @CacheEvict(
        key = "'user-name:' + #arg0",
        condition = "#result"
    )
    public boolean updateUserName(String userId, String newName) {
        log.info("사용자 이름 업데이트 - userId: {}, newName: {}", userId, newName);
        
        String key = "user:" + userId;
        if (fakeDatabase.containsKey(key)) {
            fakeDatabase.put(key, newName);
            log.info("사용자 이름 업데이트 완료 및 캐시 무효화 - userId: {}", userId);
            return true;
        }
        
        return false;
    }

    /**
     * 사용자 캐시 수동 무효화
     */
    @CacheEvict(
        key = "'user-name:' + #arg0"
    )
    public void evictUserCache(String userId) {
        log.info("사용자 캐시 무효화 - userId: {}", userId);
        // 메서드 내용 없음 - 캐시 무효화만 수행
    }

    /**
     * 캐시 조건부 적용 테스트
     */
    @Cacheable(
        key = "'user-data:' + #arg0",
        expireAfterWrite = 60L,
        condition = "#arg0 != null && #arg0.length() > 0",
        unless = "#result == null || #result.isEmpty()"
    )
    public String getUserData(String userId) {
        log.info("사용자 데이터 조회 - userId: {}", userId);
        
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        
        return "Data for user " + userId;
    }
}
