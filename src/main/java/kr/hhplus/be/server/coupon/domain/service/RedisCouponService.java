package kr.hhplus.be.server.coupon.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * Redis 기반 선착순 쿠폰 발급 서비스
 * 기존 DB 기반 로직과 병행하여 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCouponService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis 기반 선착순 쿠폰 발급 체크 (기본 명령어 사용)
     * 기존 IssueCouponService에서 DB 처리 전에 호출
     * 
     * 주의: 만료일 체크는 DB에서 수행하므로 Redis에서는 수량과 중복 발급만 체크
     */
    public CouponIssueResult checkAndIssueCoupon(Long couponId, Long userId, Integer maxIssuanceCount) {
        String issuedKey = generateIssuedKey(couponId);
        String userKey = userId.toString();

        try {
            // 1. 이미 발급받았는지 확인
            Boolean isMember = redisTemplate.opsForSet().isMember(issuedKey, userKey);
            if (Boolean.TRUE.equals(isMember)) {
                return CouponIssueResult.failure("이미 발급받은 쿠폰입니다.");
            }

            // 2. 현재 발급 수량 확인
            Long currentCount = redisTemplate.opsForSet().size(issuedKey);
            if (currentCount != null && currentCount >= maxIssuanceCount) {
                return CouponIssueResult.failure("쿠폰이 모두 소진되었습니다.");
            }

            // 3. 발급 처리 (SADD는 이미 존재하면 0, 새로 추가되면 1 반환)
            Long added = redisTemplate.opsForSet().add(issuedKey, userKey);

            if (added != null && added > 0) {
                // 4. TTL 설정 (30일)
                redisTemplate.expire(issuedKey, Duration.ofDays(30));
                log.debug("Redis 쿠폰 발급 성공 - couponId: {}, userId: {}", couponId, userId);
                return CouponIssueResult.success();
            } else {
                // 동시 요청으로 인해 이미 추가된 경우
                return CouponIssueResult.failure("이미 발급받은 쿠폰입니다.");
            }

        } catch (Exception e) {
            log.warn("Redis 쿠폰 발급 체크 실패 - couponId: {}, userId: {}", couponId, userId, e);
            // Redis 실패 시 DB 로직으로 fallback
            return CouponIssueResult.fallbackToDb();
        }
    }

    /**
     * 사용자 발급 여부 확인
     */
    public Boolean isUserIssued(Long couponId, Long userId) {
        String issuedKey = generateIssuedKey(couponId);
        
        try {
            return redisTemplate.opsForSet().isMember(issuedKey, userId.toString());
        } catch (Exception e) {
            log.warn("사용자 발급 여부 확인 실패 - couponId: {}, userId: {}", couponId, userId, e);
            return null;
        }
    }
    
    /**
     * 현재 발급 수량 조회 (빠른 실패 체크용)
     */
    public Long getCurrentIssuedCount(Long couponId) {
        String issuedKey = generateIssuedKey(couponId);
        
        try {
            return redisTemplate.opsForSet().size(issuedKey);
        } catch (Exception e) {
            log.warn("현재 발급 수량 조회 실패 - couponId: {}", couponId, e);
            return null;
        }
    }
    
    /**
     * 쿠폰 소진 여부 확인 (빠른 실패 체크용)
     */
    public Boolean isCouponExhausted(Long couponId, Integer maxIssuanceCount) {
        Long currentCount = getCurrentIssuedCount(couponId);
        if (currentCount == null) {
            return null; // Redis 오류 시 DB 체크로 넘어감
        }
        return currentCount >= maxIssuanceCount;
    }


    /**
     * Redis 기반 선착순 쿠폰 발급 체크 (최적화된 방법)
     * Redis의 기본 명령어만 사용하여 성능 최적화
     */
    public CouponIssueResult checkAndIssueCouponOptimized(Long couponId, Long userId, Integer maxIssuanceCount) {
        String issuedKey = generateIssuedKey(couponId);
        String userKey = userId.toString();
        
        try {
            // 1. SADD로 한 번에 처리 (이미 존재하면 0, 새로 추가되면 1 반환)
            Long added = redisTemplate.opsForSet().add(issuedKey, userKey);
            
            if (added != null && added > 0) {
                // 2. 새로 추가된 경우에만 수량 체크
                Long currentCount = redisTemplate.opsForSet().size(issuedKey);
                
                if (currentCount != null && currentCount > maxIssuanceCount) {
                    // 3. 수량 초과 시 제거
                    redisTemplate.opsForSet().remove(issuedKey, userKey);
                    return CouponIssueResult.failure("쿠폰이 모두 소진되었습니다.");
                }
                
                // 4. TTL 설정 (30일)
                redisTemplate.expire(issuedKey, Duration.ofDays(30));
                log.debug("Redis 쿠폰 발급 성공 (최적화) - couponId: {}, userId: {}", couponId, userId);
                return CouponIssueResult.success();
            } else {
                // 이미 발급받은 경우
                return CouponIssueResult.failure("이미 발급받은 쿠폰입니다.");
            }
            
        } catch (Exception e) {
            log.warn("Redis 쿠폰 발급 체크 실패 (최적화) - couponId: {}, userId: {}", couponId, userId, e);
            return CouponIssueResult.fallbackToDb();
        }
    }

    /**
     * 쿠폰 정보를 Redis에 캐싱
     */
    public void cacheCouponInfo(Long couponId, String name, String description, 
                               Integer discountAmount, Integer maxIssuanceCount, 
                               Integer issuedCount, String status, 
                               LocalDateTime validFrom, LocalDateTime validTo) {
        String couponKey = generateCouponInfoKey(couponId);
        
        try {
            Map<String, String> couponData = new HashMap<>();
            couponData.put("name", name);
            couponData.put("description", description != null ? description : "");
            couponData.put("discountAmount", String.valueOf(discountAmount));
            couponData.put("maxIssuanceCount", String.valueOf(maxIssuanceCount));
            couponData.put("issuedCount", String.valueOf(issuedCount));
            couponData.put("status", status);
            couponData.put("validFrom", validFrom != null ? validFrom.toString() : "");
            couponData.put("validTo", validTo != null ? validTo.toString() : "");
            
            redisTemplate.opsForHash().putAll(couponKey, couponData);
            redisTemplate.expire(couponKey, Duration.ofHours(24)); // 24시간 캐시
            
            log.debug("쿠폰 정보 캐싱 완료 - couponId: {}", couponId);
        } catch (Exception e) {
            log.warn("쿠폰 정보 캐싱 실패 - couponId: {}", couponId, e);
        }
    }

    /**
     * Redis에서 쿠폰 정보 조회
     */
    public Optional<CouponInfo> getCouponInfoFromCache(Long couponId) {
        String couponKey = generateCouponInfoKey(couponId);
        
        try {
            Map<Object, Object> couponData = redisTemplate.opsForHash().entries(couponKey);
            
            if (couponData.isEmpty()) {
                return Optional.empty();
            }
            
            CouponInfo couponInfo = new CouponInfo(
                couponId,
                (String) couponData.get("name"),
                (String) couponData.get("description"),
                Integer.valueOf((String) couponData.get("discountAmount")),
                Integer.valueOf((String) couponData.get("maxIssuanceCount")),
                Integer.valueOf((String) couponData.get("issuedCount")),
                (String) couponData.get("status"),
                parseDateTime((String) couponData.get("validFrom")),
                parseDateTime((String) couponData.get("validTo"))
            );
            
            return Optional.of(couponInfo);
        } catch (Exception e) {
            log.warn("Redis에서 쿠폰 정보 조회 실패 - couponId: {}", couponId, e);
            return Optional.empty();
        }
    }

    /**
     * Redis에서 쿠폰 정보 업데이트 (발급 수량 증가)
     */
    public void updateCouponIssuedCount(Long couponId, Integer newIssuedCount) {
        String couponKey = generateCouponInfoKey(couponId);
        
        try {
            if (newIssuedCount != null) {
                redisTemplate.opsForHash().put(couponKey, "issuedCount", String.valueOf(newIssuedCount));
            } else {
                // newIssuedCount가 null인 경우 현재 값을 증가
                redisTemplate.opsForHash().increment(couponKey, "issuedCount", 1);
            }
            log.debug("쿠폰 발급 수량 업데이트 - couponId: {}, newCount: {}", couponId, newIssuedCount);
        } catch (Exception e) {
            log.warn("쿠폰 발급 수량 업데이트 실패 - couponId: {}", couponId, e);
        }
    }
    
    /**
     * 쿠폰 발급 롤백 (실패 시 Redis에서 사용자 제거)
     */
    public void rollbackCouponIssuance(Long couponId, Long userId) {
        String issuedKey = generateIssuedKey(couponId);
        String userKey = userId.toString();
        
        try {
            Long removed = redisTemplate.opsForSet().remove(issuedKey, userKey);
            if (removed != null && removed > 0) {
                log.info("쿠폰 발급 롤백 완료 - couponId: {}, userId: {}", couponId, userId);
            }
        } catch (Exception e) {
            log.error("쿠폰 발급 롤백 실패 - couponId: {}, userId: {}", couponId, userId, e);
        }
    }

    /**
     * Redis에서 쿠폰 정보 삭제
     */
    public void deleteCouponInfo(Long couponId) {
        String couponKey = generateCouponInfoKey(couponId);
        
        try {
            redisTemplate.delete(couponKey);
            log.debug("쿠폰 정보 삭제 - couponId: {}", couponId);
        } catch (Exception e) {
            log.warn("쿠폰 정보 삭제 실패 - couponId: {}", couponId, e);
        }
    }

    private String generateIssuedKey(Long couponId) {
        return "coupon:issued:" + couponId;
    }

    private String generateCouponInfoKey(Long couponId) {
        return "coupon:info:" + couponId;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * 쿠폰 발급 결과
     */
    public static class CouponIssueResult {
        private final boolean success;
        private final boolean fallbackToDb;
        private final String errorMessage;

        private CouponIssueResult(boolean success, boolean fallbackToDb, String errorMessage) {
            this.success = success;
            this.fallbackToDb = fallbackToDb;
            this.errorMessage = errorMessage;
        }

        public static CouponIssueResult success() {
            return new CouponIssueResult(true, false, null);
        }

        public static CouponIssueResult failure(String errorMessage) {
            return new CouponIssueResult(false, false, errorMessage);
        }

        public static CouponIssueResult fallbackToDb() {
            return new CouponIssueResult(false, true, null);
        }

        public boolean isSuccess() { return success; }
        public boolean shouldFallbackToDb() { return fallbackToDb; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 쿠폰 정보 DTO
     */
    public static class CouponInfo {
        private final Long id;
        private final String name;
        private final String description;
        private final Integer discountAmount;
        private final Integer maxIssuanceCount;
        private final Integer issuedCount;
        private final String status;
        private final LocalDateTime validFrom;
        private final LocalDateTime validTo;

        public CouponInfo(Long id, String name, String description, Integer discountAmount,
                         Integer maxIssuanceCount, Integer issuedCount, String status,
                         LocalDateTime validFrom, LocalDateTime validTo) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.discountAmount = discountAmount;
            this.maxIssuanceCount = maxIssuanceCount;
            this.issuedCount = issuedCount;
            this.status = status;
            this.validFrom = validFrom;
            this.validTo = validTo;
        }

        // Getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Integer getDiscountAmount() { return discountAmount; }
        public Integer getMaxIssuanceCount() { return maxIssuanceCount; }
        public Integer getIssuedCount() { return issuedCount; }
        public String getStatus() { return status; }
        public LocalDateTime getValidFrom() { return validFrom; }
        public LocalDateTime getValidTo() { return validTo; }
    }


}