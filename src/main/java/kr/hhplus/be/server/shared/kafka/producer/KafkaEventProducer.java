package kr.hhplus.be.server.shared.kafka.producer;

import kr.hhplus.be.server.shared.kafka.message.DataPlatformMessage;
import kr.hhplus.be.server.shared.kafka.message.OrderCompletedMessage;
import kr.hhplus.be.server.shared.kafka.message.ProductRankingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka Event Producer
 * 주문 관련 이벤트를 Kafka로 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-completed}")
    private String orderCompletedTopic;

    @Value("${kafka.topics.product-ranking}")
    private String productRankingTopic;

    @Value("${kafka.topics.data-platform-transfer}")
    private String dataPlatformTransferTopic;

    /**
     * 주문 완료 이벤트 발행
     * 파티션 키: userId (같은 사용자의 주문은 순서 보장)
     */
    public void publishOrderCompleted(OrderCompletedMessage message) {
        try {
            String key = "user-" + message.getUserId();
            log.info("Publishing order completed event - orderId: {}, userId: {}, topic: {}", 
                    message.getOrderId(), message.getUserId(), orderCompletedTopic);
            
            kafkaTemplate.send(orderCompletedTopic, key, message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Order completed event published successfully - orderId: {}, offset: {}",
                                    message.getOrderId(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish order completed event - orderId: {}", 
                                    message.getOrderId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing order completed event - orderId: {}", message.getOrderId(), e);
        }
    }

    /**
     * 상품 랭킹 업데이트 이벤트 발행
     * 파티션 키: productId (같은 상품의 랭킹 업데이트는 순서 보장)
     */
    public void publishProductRanking(ProductRankingMessage message) {
        try {
            String key = "product-" + message.getProductId();
            log.info("Publishing product ranking event - productId: {}, orderId: {}, topic: {}", 
                    message.getProductId(), message.getOrderId(), productRankingTopic);
            
            kafkaTemplate.send(productRankingTopic, key, message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Product ranking event published successfully - productId: {}, offset: {}",
                                    message.getProductId(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish product ranking event - productId: {}", 
                                    message.getProductId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing product ranking event - productId: {}", message.getProductId(), e);
        }
    }

    /**
     * 데이터 플랫폼 전송 이벤트 발행
     * 파티션 키: orderId (주문별 순서 보장)
     */
    public void publishDataPlatformTransfer(DataPlatformMessage message) {
        try {
            String key = "order-" + message.getOrderId();
            log.info("Publishing data platform transfer event - orderId: {}, topic: {}", 
                    message.getOrderId(), dataPlatformTransferTopic);
            
            kafkaTemplate.send(dataPlatformTransferTopic, key, message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Data platform transfer event published successfully - orderId: {}, offset: {}",
                                    message.getOrderId(), result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish data platform transfer event - orderId: {}", 
                                    message.getOrderId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing data platform transfer event - orderId: {}", message.getOrderId(), e);
        }
    }
}