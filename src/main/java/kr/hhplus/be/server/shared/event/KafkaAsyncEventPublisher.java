package kr.hhplus.be.server.shared.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka 기반 비동기 이벤트 발행자
 * 차주에 구현할 예정인 Kafka 연동 구현체
 * 
 * 현재는 Mock 구현으로, 실제 Kafka 연동시에는 KafkaTemplate을 주입받아 사용
 */
@Slf4j
// @Component - EventPublisherConfig에서 빈 생성
public class KafkaAsyncEventPublisher implements AsyncEventPublisher {
    
    // TODO: 차주에 KafkaTemplate 주입
    // private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void publishAsync(Object event, String topic) {
        log.debug("Kafka 비동기 이벤트 발행 (Mock) - event: {}, topic: {}", 
                 event.getClass().getSimpleName(), topic);
        
        // TODO: 차주에 실제 Kafka 발행 로직 구현
        // kafkaTemplate.send(topic, event);
        
        // 현재는 Mock으로 로그만 출력
        log.info("Kafka 이벤트 발행 (Mock) - topic: {}, event: {}", 
                topic, event.getClass().getSimpleName());
    }
    
    @Override
    public String getDefaultTopic() {
        return "ecommerce-events";
    }
}