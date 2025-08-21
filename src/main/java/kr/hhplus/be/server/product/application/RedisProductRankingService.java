package kr.hhplus.be.server.product.application;

import kr.hhplus.be.server.product.application.port.in.ProductRankingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis 기반 상품 랭킹 서비스 (Primary)
 * 주문 완료 시점에 상품별 판매량을 실시간 집계
 */
@Slf4j
@Service("redisProductRankingService")
@Primary
@RequiredArgsConstructor
public class RedisProductRankingService implements ProductRankingUseCase {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void updateProductRanking(Long productId, Integer quantity) {
        LocalDate today = LocalDate.now();
        
        // 최근 3일간 각각 업데이트
        for (int i = 0; i < 3; i++) {
            LocalDate date = today.minusDays(i);
            String key = generateDailyRankingKey(date);
            
            try {
                redisTemplate.opsForZSet().incrementScore(key, productId.toString(), quantity.doubleValue());
                // TTL 설정 (4일 후 자동 삭제)
                redisTemplate.expire(key, java.time.Duration.ofDays(4));
                
                log.debug("상품 랭킹 업데이트 - productId: {}, date: {}, quantity: {}", productId, date, quantity);
            } catch (Exception e) {
                log.warn("상품 랭킹 업데이트 실패 - productId: {}, date: {}", productId, date, e);
                throw new RuntimeException("Redis 랭킹 업데이트 실패", e);
            }
        }
    }

    /**
     * 인기상품 TOP N 상품 ID 목록 조회
     * 최근 3일간 판매량을 기준으로 상위 N개 상품 ID를 반환
     * 
     * @param limit 조회할 상품 개수
     * @return 판매량 순위대로 정렬된 상품 ID 목록
     */
    @Override
    public List<Long> getTopProductIds(int limit) {
        LocalDate today = LocalDate.now();
        String aggregateKey = "product:ranking:recent3days:" + today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        try {
            // 최근 3일 데이터가 이미 집계되어 있는지 확인
            if (!redisTemplate.hasKey(aggregateKey)) {
                aggregateRecentDaysRanking(aggregateKey, today, 3);
            }
            
            // TOP N 조회 (점수 높은 순)
            Set<Object> topProducts = redisTemplate.opsForZSet().reverseRange(aggregateKey, 0, limit - 1);
            
            return topProducts.stream()
                    .map(o -> Long.parseLong(o.toString()))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.warn("Redis에서 인기상품 조회 실패", e);
            throw new RuntimeException("Redis 랭킹 조회 실패", e);
        }
    }

    @Override
    public Long getProductRank(Long productId) {
        LocalDate today = LocalDate.now();
        String aggregateKey = "product:ranking:recent3days:" + today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        try {
            if (!redisTemplate.hasKey(aggregateKey)) {
                aggregateRecentDaysRanking(aggregateKey, today, 3);
            }
            
            return redisTemplate.opsForZSet().reverseRank(aggregateKey, productId.toString());
        } catch (Exception e) {
            log.warn("상품 랭킹 조회 실패 - productId: {}", productId, e);
            throw new RuntimeException("Redis 랭킹 조회 실패", e);
        }
    }

    @Override
    public Double getProductSalesScore(Long productId) {
        LocalDate today = LocalDate.now();
        String aggregateKey = "product:ranking:recent3days:" + today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        try {
            if (!redisTemplate.hasKey(aggregateKey)) {
                aggregateRecentDaysRanking(aggregateKey, today, 3);
            }
            
            return redisTemplate.opsForZSet().score(aggregateKey, productId.toString());
        } catch (Exception e) {
            log.warn("상품 판매량 점수 조회 실패 - productId: {}", productId, e);
            throw new RuntimeException("Redis 판매량 점수 조회 실패", e);
        }
    }

    /**
     * 최근 N일간 랭킹 집계
     * 일별 랭킹을 합쳐서 통합 랭킹 생성
     */
    private void aggregateRecentDaysRanking(String aggregateKey, LocalDate baseDate, int days) {
        try {
            // 최근 N일간의 키들 수집
            String[] dailyKeys = new String[days];
            for (int i = 0; i < days; i++) {
                dailyKeys[i] = generateDailyRankingKey(baseDate.minusDays(i));
            }
            
            // ZUNIONSTORE로 여러 일별 랭킹을 합산
            if (dailyKeys.length > 0) {
                redisTemplate.opsForZSet().unionAndStore(dailyKeys[0], List.of(dailyKeys).subList(1, dailyKeys.length), aggregateKey);
                // 집계 결과는 1시간 TTL
                redisTemplate.expire(aggregateKey, java.time.Duration.ofHours(1));
            }
            
            log.debug("최근 {}일 랭킹 집계 완료 - key: {}", days, aggregateKey);
        } catch (Exception e) {
            log.warn("랭킹 집계 실패 - key: {}", aggregateKey, e);
            throw new RuntimeException("Redis 랭킹 집계 실패", e);
        }
    }

    /**
     * 일별 랭킹 키 생성
     */
    private String generateDailyRankingKey(LocalDate date) {
        return "product:ranking:daily:" + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}