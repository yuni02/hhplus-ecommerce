package kr.hhplus.be.server.order.domain.service;

import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.event.OrderCompletedEvent;
import kr.hhplus.be.server.shared.kafka.message.DataPlatformMessage;
import kr.hhplus.be.server.shared.kafka.message.OrderCompletedMessage;
import kr.hhplus.be.server.shared.kafka.message.ProductRankingMessage;
import kr.hhplus.be.server.shared.kafka.producer.KafkaEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka 기반 주문 이벤트 핸들러
 * event.publisher.type=kafka일 때 활성화
 * 트랜잭션 완료 후 Kafka로 이벤트 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.publisher.type", havingValue = "kafka")
public class KafkaOrderEventHandler {

    private final KafkaEventProducer kafkaEventProducer;

    /**
     * 주문 완료 이벤트를 Kafka로 발행
     * 트랜잭션 완료 후 실행하여 데이터 정합성 보장
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            log.info("Publishing order events to Kafka - orderId: {}", event.getOrderId());

            // 1. 주문 완료 메시지 발행
            publishOrderCompletedMessage(event);

            // 2. 상품 랭킹 업데이트 메시지들 발행
            publishProductRankingMessages(event);

            // 3. 데이터 플랫폼 전송 메시지 발행
            publishDataPlatformMessage(event);

            log.info("All order events published to Kafka successfully - orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to publish order events to Kafka - orderId: {}", event.getOrderId(), e);
            // Kafka 발행 실패는 메인 트랜잭션에 영향을 주지 않음
            // 필요시 재시도 로직이나 알림 추가 가능
        }
    }

    /**
     * 주문 완료 메시지 발행
     */
    private void publishOrderCompletedMessage(OrderCompletedEvent event) {
        List<OrderCompletedMessage.OrderItemMessage> orderItems = event.getOrderItems().stream()
                .map(this::convertToOrderItemMessage)
                .toList();

        OrderCompletedMessage message = OrderCompletedMessage.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .totalAmount(event.getTotalAmount())
                .discountedAmount(event.getDiscountedAmount())
                .discountAmount(event.getDiscountAmount())
                .userCouponId(event.getUserCouponId())
                .status("COMPLETED")
                .orderItems(orderItems)
                .orderedAt(event.getOrderedAt())
                .eventOccurredAt(LocalDateTime.now())
                .build();

        kafkaEventProducer.publishOrderCompleted(message);
    }

    /**
     * 상품 랭킹 업데이트 메시지들 발행
     * 주문한 각 상품별로 개별 메시지 발행
     */
    private void publishProductRankingMessages(OrderCompletedEvent event) {
        for (OrderItem item : event.getOrderItems()) {
            ProductRankingMessage message = ProductRankingMessage.builder()
                    .orderId(event.getOrderId())
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .soldQuantity(item.getQuantity().longValue())
                    .eventOccurredAt(LocalDateTime.now())
                    .build();

            kafkaEventProducer.publishProductRanking(message);
        }
    }

    /**
     * 데이터 플랫폼 전송 메시지 발행
     */
    private void publishDataPlatformMessage(OrderCompletedEvent event) {
        List<DataPlatformMessage.DataPlatformOrderItem> orderItems = event.getOrderItems().stream()
                .map(this::convertToDataPlatformItem)
                .toList();

        DataPlatformMessage message = DataPlatformMessage.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .userName("User-" + event.getUserId()) // 실제로는 User 정보 조회 필요
                .totalAmount(event.getTotalAmount())
                .discountedAmount(event.getDiscountedAmount())
                .discountAmount(event.getDiscountAmount())
                .orderStatus("COMPLETED")
                .paymentMethod("POINT") // 현재는 포인트 결제만 지원
                .orderItems(orderItems)
                .orderedAt(event.getOrderedAt())
                .processedAt(LocalDateTime.now())
                .build();

        kafkaEventProducer.publishDataPlatformTransfer(message);
    }

    /**
     * OrderItem을 OrderCompletedMessage.OrderItemMessage로 변환
     */
    private OrderCompletedMessage.OrderItemMessage convertToOrderItemMessage(OrderItem item) {
        return OrderCompletedMessage.OrderItemMessage.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }

    /**
     * OrderItem을 DataPlatformMessage.DataPlatformOrderItem으로 변환
     */
    private DataPlatformMessage.DataPlatformOrderItem convertToDataPlatformItem(OrderItem item) {
        return DataPlatformMessage.DataPlatformOrderItem.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .category("DEFAULT") // 실제로는 Product 정보 조회 필요
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}