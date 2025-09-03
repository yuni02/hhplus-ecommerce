package kr.hhplus.be.server.shared.kafka.consumer;

import kr.hhplus.be.server.order.application.DataPlatformService;
import kr.hhplus.be.server.shared.kafka.message.DataPlatformMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 데이터 플랫폼 전송 Consumer
 * 주문 데이터를 외부 데이터 플랫폼으로 전송 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformConsumer {

    private final DataPlatformService dataPlatformService;

    @KafkaListener(
            topics = "${kafka.topics.data-platform-transfer}",
            groupId = "data-platform-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDataPlatformTransfer(
            @Payload DataPlatformMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received data platform transfer message - orderId: {}, userId: {}, topic: {}, partition: {}, offset: {}",
                    message.getOrderId(), message.getUserId(), topic, partition, offset);

            // 데이터 플랫폼으로 주문 데이터 전송
            boolean success = dataPlatformService.sendOrderData(message);
            
            if (success) {
                log.info("Data platform transfer completed successfully - orderId: {}", message.getOrderId());
                acknowledgment.acknowledge();
            } else {
                log.warn("Data platform transfer failed - orderId: {}, will retry", message.getOrderId());
                // 실패 시 재시도를 위해 acknowledge하지 않음
                // Kafka에서 자동으로 재시도됨
            }

        } catch (Exception e) {
            log.error("Failed to process data platform transfer message - orderId: {}",
                    message.getOrderId(), e);
            
            // 재시도 가능한 예외인지 판단
            if (isRetryableException(e)) {
                log.warn("Retryable exception occurred - orderId: {}, will retry", message.getOrderId());
                // acknowledge하지 않아 재시도됨
            } else {
                log.error("Non-retryable exception occurred - orderId: {}, skipping message", message.getOrderId());
                acknowledgment.acknowledge(); // 스킵
            }
        }
    }
    
    private boolean isRetryableException(Exception e) {
        // 네트워크 관련 예외는 재시도 가능
        return e.getCause() != null && 
               (e.getCause().getClass().getSimpleName().contains("Connection") ||
                e.getCause().getClass().getSimpleName().contains("Timeout"));
    }
}