package kr.hhplus.be.server.order.domain.service;


import kr.hhplus.be.server.order.domain.OrderItem;
import kr.hhplus.be.server.order.domain.event.DataPlatformTransferRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 데이터 플랫폼 이벤트 핸들러
 * AsyncEventPublisher를 통해 전달된 데이터 플랫폼 전송 이벤트 처리
 * 
 * 차주에 Kafka로 변경되면 이 핸들러는 별도 Consumer 서비스로 분리될 예정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {
    
    private final DataPlatformService dataPlatformService;
    
    /**
     * 데이터 플랫폼 전송 요청 이벤트 처리
     * Spring Event 환경에서는 @EventListener로 처리
     * Kafka 환경에서는 @KafkaListener로 변경 예정
     */
    @Async("orderEventExecutor")
    @EventListener
    public void handleDataPlatformTransfer(DataPlatformTransferRequestedEvent event) {
        try {
            log.info("데이터 플랫폼 전송 이벤트 처리 시작 - orderId: {}", event.getOrderId());
            
            // 이벤트 데이터를 DTO로 변환
            DataPlatformOrderResponse orderData = convertToDataPlatformFormat(event);
            
            // 데이터 플랫폼으로 전송
            boolean success = dataPlatformService.sendOrderData(orderData);
            
            if (success) {
                log.info("데이터 플랫폼 전송 성공 - orderId: {}", event.getOrderId());
            } else {
                log.warn("데이터 플랫폼 전송 실패 - orderId: {}", event.getOrderId());
                // TODO: 실패 시 재시도 로직 또는 Dead Letter Queue 처리
            }
            
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 중 예외 발생 - orderId: {}", event.getOrderId(), e);
            // TODO: 예외 발생 시 알림 또는 모니터링 시스템에 전송
        }
    }
    
    /**
     * 이벤트 데이터를 데이터 플랫폼 DTO로 변환
     */
    private DataPlatformOrderResponse convertToDataPlatformFormat(DataPlatformTransferRequestedEvent event) {
        List<DataPlatformOrderItemResponse> items = event.getOrderItems().stream()
                .map(this::convertOrderItem)
                .toList();
        
        return DataPlatformOrderResponse.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .orderItems(items)
                .totalAmount(event.getTotalAmount())
                .discountedAmount(event.getDiscountedAmount())
                .discountAmount(event.getDiscountAmount())
                .userCouponId(event.getUserCouponId())
                .orderedAt(event.getOrderedAt())
                .occurredAt(event.getOccurredAt())
                .build();
    }
    
    /**
     * 주문 아이템을 데이터 플랫폼 형식으로 변환
     */
    private DataPlatformOrderItemResponse convertOrderItem(OrderItem item) {
        return DataPlatformOrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
    
    // DTO 클래스들 - OrderCompletedEventHandler에서 이동
    @lombok.Builder
    @lombok.Getter
    public static class DataPlatformOrderResponse {
        private final Long orderId;
        private final Long userId;
        private final List<DataPlatformOrderItemResponse> orderItems;
        private final BigDecimal totalAmount;
        private final BigDecimal discountedAmount;
        private final BigDecimal discountAmount;
        private final Long userCouponId;
        private final java.time.LocalDateTime orderedAt;
        private final java.time.LocalDateTime occurredAt;
    }

    @lombok.Builder
    @lombok.Getter
    public static class DataPlatformOrderItemResponse {
        private final Long productId;
        private final String productName;
        private final Integer quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal totalPrice;
    }
}