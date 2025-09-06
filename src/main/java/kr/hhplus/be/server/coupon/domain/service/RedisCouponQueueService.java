package kr.hhplus.be.server.coupon.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Redis 기반 쿠폰 대기열 서비스
 * 선착순 쿠폰 발급을 위한 트래픽 제어 및 대기열 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCouponQueueService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 쿠폰 대기열에 사용자 추가
     * @return true: 대기열 추가 성공, false: 이미 대기열에 있음
     */
    public boolean addToQueue(Long couponId, Long userId) {
        String queueKey = generateQueueKey(couponId);
        String userKey = userId.toString();
        
        try {
            // 이미 대기열에 있는지 확인
            Double existingScore = redisTemplate.opsForZSet().score(queueKey, userKey);
            if (existingScore != null) {
                log.debug("사용자가 이미 대기열에 존재 - couponId: {}, userId: {}", couponId, userId);
                return false;
            }
            
            // 대기열에 추가 (score = 현재 시간)
            double score = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            Boolean added = redisTemplate.opsForZSet().add(queueKey, userKey, score);
            
            if (Boolean.TRUE.equals(added)) {
                // TTL 설정 (쿠폰 발급 기간 + 1시간)
                redisTemplate.expire(queueKey, Duration.ofHours(25));
                log.debug("대기열 추가 성공 - couponId: {}, userId: {}, score: {}", couponId, userId, score);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("대기열 추가 실패 - couponId: {}, userId: {}", couponId, userId, e);
            return false;
        }
    }


    /**
     * 대기열에서 다음 처리할 사용자 조회 및 제거 (atomic)
     */
    public Long getNextUserFromQueue(Long couponId) {
        String queueKey = generateQueueKey(couponId);
        
        try {
            // ZPOPMIN을 사용하여 가장 작은 score(가장 오래된)를 가진 사용자를 제거하고 반환
            org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> result = 
                redisTemplate.opsForZSet().popMin(queueKey);
            if (result != null && result.getValue() != null) {
                String userId = result.getValue().toString();
                log.debug("대기열에서 사용자 제거 - couponId: {}, userId: {}", couponId, userId);
                return Long.parseLong(userId);
            }
            return null;
            
        } catch (Exception e) {
            log.error("대기열에서 사용자 조회 실패 - couponId: {}", couponId, e);
            return null;
        }
    }

    /**
     * 스케줄러용 - 대기열에서 사용자 ID를 꺼냄 (별칭 메서드)
     */
    public Long pollFromQueue(Long couponId) {
        return getNextUserFromQueue(couponId);
    }

    /**
     * 대기열에서 사용자 제거
     */
    public boolean removeFromQueue(Long couponId, Long userId) {
        String queueKey = generateQueueKey(couponId);
        String userKey = userId.toString();
        
        try {
            Long removed = redisTemplate.opsForZSet().remove(queueKey, userKey);
            log.debug("대기열에서 사용자 제거 - couponId: {}, userId: {}, removed: {}", couponId, userId, removed);
            return removed != null && removed > 0;
            
        } catch (Exception e) {
            log.error("대기열에서 사용자 제거 실패 - couponId: {}, userId: {}", couponId, userId, e);
            return false;
        }
    }

    /**
     * 대기열 크기 조회
     */
    public Long getQueueSize(Long couponId) {
        String queueKey = generateQueueKey(couponId);
        
        try {
            return redisTemplate.opsForZSet().size(queueKey);
        } catch (Exception e) {
            log.error("대기열 크기 조회 실패 - couponId: {}", couponId, e);
            return 0L;
        }
    }

    /**
     * 사용자의 대기열 순서 조회
     */
    public Long getUserQueuePosition(Long couponId, Long userId) {
        String queueKey = generateQueueKey(couponId);
        String userKey = userId.toString();
        
        try {
            Long rank = redisTemplate.opsForZSet().rank(queueKey, userKey);
            return rank != null ? rank + 1 : null; // 1-based 순서
        } catch (Exception e) {
            log.error("사용자 대기열 순서 조회 실패 - couponId: {}, userId: {}", couponId, userId, e);
            return null;
        }
    }

    /**
     * 쿠폰 발급 결과 저장
     */
    public void saveIssueResult(Long couponId, Long userId, boolean success, String message) {
        String resultKey = generateResultKey(couponId, userId);
        
        try {
            CouponIssueResult result = new CouponIssueResult(success, message, LocalDateTime.now());
            redisTemplate.opsForValue().set(resultKey, result, Duration.ofMinutes(30));
            log.debug("쿠폰 발급 결과 저장 - couponId: {}, userId: {}, success: {}", couponId, userId, success);
        } catch (Exception e) {
            log.error("쿠폰 발급 결과 저장 실패 - couponId: {}, userId: {}", couponId, userId, e);
        }
    }

    /**
     * 쿠폰 발급 결과 조회
     */
    public CouponIssueResult getIssueResult(Long couponId, Long userId) {
        String resultKey = generateResultKey(couponId, userId);
        
        try {
            return (CouponIssueResult) redisTemplate.opsForValue().get(resultKey);
        } catch (Exception e) {
            log.error("쿠폰 발급 결과 조회 실패 - couponId: {}, userId: {}", couponId, userId, e);
            return null;
        }
    }

    /**
     * 대기열 키 생성
     */
    private String generateQueueKey(Long couponId) {
        return "coupon:queue:" + couponId;
    }

    /**
     * 결과 키 생성
     */
    private String generateResultKey(Long couponId, Long userId) {
        return "coupon:result:" + couponId + ":" + userId;
    }

    /**
     * 쿠폰 발급 결과
     */
    public static class CouponIssueResult {
        private final boolean success;
        private final String message;
        private final LocalDateTime processedAt;

        public CouponIssueResult(boolean success, String message, LocalDateTime processedAt) {
            this.success = success;
            this.message = message;
            this.processedAt = processedAt;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public LocalDateTime getProcessedAt() { return processedAt; }
    }
}
