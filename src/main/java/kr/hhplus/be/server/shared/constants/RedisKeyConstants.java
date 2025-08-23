package kr.hhplus.be.server.shared.constants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class RedisKeyConstants {
    
    private RedisKeyConstants() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }
    
    // Redis 키 패턴 상수
    private static final String PRODUCT_RANKING_DAILY_PREFIX = "product:ranking:daily:";
    private static final String PRODUCT_RANKING_RECENT_3DAYS_PREFIX = "product:ranking:recent3days:";
    
    /**
     * 일별 상품 랭킹 키 생성
     * @param date 날짜
     * @return Redis 키 (예: product:ranking:daily:2024-08-21)
     */
    public static String getProductRankingDailyKey(LocalDate date) {
        return PRODUCT_RANKING_DAILY_PREFIX + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    /**
     * 최근 3일 통합 상품 랭킹 키 생성
     * @param date 날짜
     * @return Redis 키 (예: product:ranking:recent3days:2024-08-21)
     */
    public static String getProductRankingRecent3DaysKey(LocalDate date) {
        return PRODUCT_RANKING_RECENT_3DAYS_PREFIX + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}