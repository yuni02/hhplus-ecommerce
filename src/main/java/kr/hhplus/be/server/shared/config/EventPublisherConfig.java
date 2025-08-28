package kr.hhplus.be.server.shared.config;

import kr.hhplus.be.server.shared.event.AsyncEventPublisher;
import kr.hhplus.be.server.shared.event.KafkaAsyncEventPublisher;
import kr.hhplus.be.server.shared.event.SpringAsyncEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 이벤트 발행자 설정
 * application.yml의 event.publisher.type 설정에 따라 구현체 선택
 * 
 * 설정값:
 * - spring (기본값): SpringAsyncEventPublisher 사용
 * - kafka: KafkaAsyncEventPublisher 사용 (차주 구현 예정)
 */
@Configuration
public class EventPublisherConfig {
    
    /**
     * Spring Event 기반 발행자 (기본값)
     * event.publisher.type이 설정되지 않았거나 spring인 경우 사용
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(AsyncEventPublisher.class)
    @ConditionalOnProperty(name = "event.publisher.type", havingValue = "spring", matchIfMissing = true)
    public AsyncEventPublisher springAsyncEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringAsyncEventPublisher(applicationEventPublisher);
    }
    
    /**
     * Kafka 기반 발행자 
     * event.publisher.type=kafka인 경우 사용
     */
    @Bean
    @ConditionalOnProperty(name = "event.publisher.type", havingValue = "kafka")
    public AsyncEventPublisher kafkaAsyncEventPublisher() {
        return new KafkaAsyncEventPublisher();
    }
}