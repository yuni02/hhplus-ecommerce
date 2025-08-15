package kr.hhplus.be.server.shared.cache;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class CacheKeyGenerator {

    private static final String SEPARATOR = ":";
    private static final String VERSION = "v1";

    /**
     * 기본 캐시 키 생성
     */
    public String generateKey(String domain, String... parts) {
        return String.join(SEPARATOR, 
            VERSION, domain, Arrays.stream(parts).collect(Collectors.joining(SEPARATOR)));
    }

    /**
     * 사용자 관련 캐시 키 생성
     */
    public String generateUserKey(String operation, Long userId) {
        return generateKey("user", operation, userId.toString());
    }

    /**
     * 잔액 관련 캐시 키 생성
     */
    public String generateBalanceKey(String operation, Long userId) {
        return generateKey("balance", operation, userId.toString());
    }

    /**
     * 쿠폰 관련 캐시 키 생성
     */
    public String generateCouponKey(String operation, Long couponId) {
        return generateKey("coupon", operation, couponId.toString());
    }

    /**
     * 사용자 쿠폰 관련 캐시 키 생성
     */
    public String generateUserCouponKey(String operation, Long userId, Long couponId) {
        return generateKey("user-coupon", operation, userId.toString(), couponId.toString());
    }

    /**
     * 상품 관련 캐시 키 생성
     */
    public String generateProductKey(String operation, Long productId) {
        return generateKey("product", operation, productId.toString());
    }

    /**
     * 주문 관련 캐시 키 생성
     */
    public String generateOrderKey(String operation, Long orderId) {
        return generateKey("order", operation, orderId.toString());
    }

    /**
     * 상품 통계 관련 캐시 키 생성
     */
    public String generateProductStatsKey(String operation, String period) {
        return generateKey("product-stats", operation, period);
    }

    /**
     * 락 키 생성
     */
    public String generateLockKey(String domain, String operation, String... parts) {
        String[] allParts = new String[parts.length + 1];
        allParts[0] = operation;
        System.arraycopy(parts, 0, allParts, 1, parts.length);
        return "lock:" + generateKey(domain, allParts);
    }
}
