package kr.hhplus.be.server.shared.kafka.consumer;

import kr.hhplus.be.server.product.domain.service.RedisProductRankingService;
import kr.hhplus.be.server.shared.kafka.message.ProductRankingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 상품 랭킹 업데이트 Consumer
 * 주문 완료 시 상품 판매량 증가 및 랭킹 업데이트 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRankingConsumer {

    private final RedisProductRankingService productRankingService;

    @KafkaListener(
            topics = "${kafka.topics.product-ranking}",
            groupId = "product-ranking-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductRankingUpdate(
            @Payload ProductRankingMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received product ranking message - productId: {}, orderId: {}, quantity: {}, topic: {}, partition: {}, offset: {}",
                    message.getProductId(), message.getOrderId(), message.getQuantity(), topic, partition, offset);

            // 상품 판매량 증가 및 랭킹 업데이트
            productRankingService.updateProductRanking(
                    message.getProductId(),
                    message.getQuantity()
            );

            // 비동기 커밋
            acknowledgment.acknowledge();
            
            log.info("Product ranking updated and async commit done - productId: {}, orderId: {}",
                    message.getProductId(), message.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process product ranking message - productId: {}, orderId: {}",
                    message.getProductId(), message.getOrderId(), e);
            // 에러 발생 시 커밋하지 않음 (재처리를 위해)
            // acknowledgment를 호출하지 않으면 메시지는 재처리됨
        }
    }
}