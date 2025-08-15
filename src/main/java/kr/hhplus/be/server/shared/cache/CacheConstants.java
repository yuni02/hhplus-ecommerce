package kr.hhplus.be.server.shared.cache;

import java.time.Duration;

public class CacheConstants {

    // 캐시 만료 시간
    public static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);
    public static final Duration BALANCE_CACHE_TTL = Duration.ofMinutes(10);
    public static final Duration COUPON_CACHE_TTL = Duration.ofHours(1);
    public static final Duration USER_COUPON_CACHE_TTL = Duration.ofMinutes(15);
    public static final Duration PRODUCT_CACHE_TTL = Duration.ofHours(2);
    public static final Duration ORDER_CACHE_TTL = Duration.ofMinutes(5);
    public static final Duration PRODUCT_STATS_CACHE_TTL = Duration.ofHours(6);

    // 락 타임아웃
    public static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration COUPON_ISSUE_LOCK_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration BALANCE_CHARGE_LOCK_TIMEOUT = Duration.ofSeconds(15);
    public static final Duration ORDER_CREATE_LOCK_TIMEOUT = Duration.ofSeconds(20);

    // 락 대기 시간
    public static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration COUPON_ISSUE_WAIT_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration BALANCE_CHARGE_WAIT_TIMEOUT = Duration.ofSeconds(8);
    public static final Duration ORDER_CREATE_WAIT_TIMEOUT = Duration.ofSeconds(15);

    // 캐시 키 접두사
    public static final String USER_PREFIX = "user";
    public static final String BALANCE_PREFIX = "balance";
    public static final String COUPON_PREFIX = "coupon";
    public static final String USER_COUPON_PREFIX = "user-coupon";
    public static final String PRODUCT_PREFIX = "product";
    public static final String ORDER_PREFIX = "order";
    public static final String PRODUCT_STATS_PREFIX = "product-stats";

    // 캐시 키 작업
    public static final String OPERATION_GET = "get";
    public static final String OPERATION_CREATE = "create";
    public static final String OPERATION_UPDATE = "update";
    public static final String OPERATION_DELETE = "delete";
    public static final String OPERATION_ISSUE = "issue";
    public static final String OPERATION_USE = "use";
    public static final String OPERATION_CHARGE = "charge";
    public static final String OPERATION_DEDUCT = "deduct";
    public static final String OPERATION_STATS = "stats";
}
