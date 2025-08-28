package kr.hhplus.be.server.shared.event;

/**
 * 비동기 이벤트 발행 인터페이스
 * DIP(의존성 역전 원칙)를 적용하여 Spring Event와 Kafka를 교체할 수 있도록 추상화
 */
public interface AsyncEventPublisher {
    
    /**
     * 비동기 이벤트 발행
     * 
     * @param event 발행할 이벤트
     * @param topic 이벤트 토픽/채널 (Spring Event에서는 무시됨, Kafka에서는 토픽명으로 사용)
     */
    void publishAsync(Object event, String topic);
    
    /**
     * 비동기 이벤트 발행 (기본 토픽)
     * 
     * @param event 발행할 이벤트
     */
    default void publishAsync(Object event) {
        publishAsync(event, getDefaultTopic());
    }
    
    /**
     * 기본 토픽명 반환
     * 각 구현체에서 오버라이드하여 사용
     */
    default String getDefaultTopic() {
        return "default-topic";
    }
}