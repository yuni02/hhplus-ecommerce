package kr.hhplus.be.server.shared.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring Application Event 기반 비동기 이벤트 발행자
 * 현재 사용중인 구현체
 */
@Slf4j
// @Component - EventPublisherConfig에서 빈 생성
@RequiredArgsConstructor
public class SpringAsyncEventPublisher implements AsyncEventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Override
    public void publishAsync(Object event, String topic) {
        log.debug("Spring 비동기 이벤트 발행 - event: {}, topic: {}", 
                 event.getClass().getSimpleName(), topic);
        
        // Spring Event에서는 topic을 사용하지 않고 ApplicationEventPublisher를 통해 발행
        applicationEventPublisher.publishEvent(event);
        
        log.debug("Spring 이벤트 발행 완료 - event: {}", event.getClass().getSimpleName());
    }
    
    @Override
    public String getDefaultTopic() {
        return "spring-events";
    }
}