package kr.hhplus.be.server.shared.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheManager(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 캐시에 데이터 저장
     */
    public <T> void set(String key, T value, Duration expiration) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, expiration);
            log.debug("Cache set - key: {}, expiration: {}", key, expiration);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize value for key: {}", key, e);
        }
    }

    /**
     * 캐시에서 데이터 조회
     */
    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            
            if (value instanceof String) {
                T result = objectMapper.readValue((String) value, clazz);
                return Optional.of(result);
            }
            
            return Optional.of(clazz.cast(value));
        } catch (Exception e) {
            log.error("Failed to deserialize value for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * 캐시 삭제
     */
    public void delete(String key) {
        redisTemplate.delete(key);
        log.debug("Cache deleted - key: {}", key);
    }

    /**
     * 캐시 존재 여부 확인
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 캐시 만료시간 설정
     */
    public void expire(String key, Duration expiration) {
        redisTemplate.expire(key, expiration);
    }

    /**
     * 원자적 증가
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 원자적 감소
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, -delta);
    }
}
