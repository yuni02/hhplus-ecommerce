package kr.hhplus.be.server.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * 도메인별로 분리된 스레드 풀 구성
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 쿠폰 발급 전용 스레드 풀
     * 대용량 트래픽 처리에 최적화
     */
    @Bean("couponIssueExecutor")
    public Executor couponIssueExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);           // 기본 스레드 수
        executor.setMaxPoolSize(20);           // 최대 스레드 수
        executor.setQueueCapacity(100);        // 대기열 크기
        executor.setThreadNamePrefix("coupon-issue-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 상품 랭킹 업데이트 전용 스레드 풀
     * Redis 작업에 최적화
     */
    @Bean("productRankingExecutor")
    public Executor productRankingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);           // 기본 스레드 수
        executor.setMaxPoolSize(10);           // 최대 스레드 수
        executor.setQueueCapacity(50);         // 대기열 크기
        executor.setThreadNamePrefix("product-ranking-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 주문 이벤트 처리 전용 스레드 풀
     * 데이터 플랫폼 전송 및 알림톡 발송에 최적화
     */
    @Bean("orderEventExecutor")
    public Executor orderEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);           // 기본 스레드 수
        executor.setMaxPoolSize(8);            // 최대 스레드 수
        executor.setQueueCapacity(30);         // 대기열 크기
        executor.setThreadNamePrefix("order-event-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
