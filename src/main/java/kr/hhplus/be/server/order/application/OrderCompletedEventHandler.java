package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.domain.OrderCompletedEvent;
import kr.hhplus.be.server.order.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 주문 완료 이벤트 핸들러
 * 트랜잭션 완료 후 비동기로 부가 로직 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.publisher.type", havingValue = "spring", matchIfMissing = true)
public class OrderCompletedEventHandler {

    private final DataPlatformService dataPlatformService;

    /**
     * 데이터 플랫폼 전송 핸들러
     * AFTER_COMMIT으로 트랜잭션 완료 후 실행
     */
    @Async("orderEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDataPlatformTransfer(OrderCompletedEvent event) {
        try {
            log.info("데이터 플랫폼 전송 시작 - orderId: {}", event.getOrderId());
            
            // 주문 데이터를 데이터 플랫폼 형식으로 변환
            DataPlatformOrderResponse orderData = convertToDataPlatformFormat(event);
            
            // 데이터 플랫폼으로 전송 (Mock API 호출)
            boolean success = dataPlatformService.sendOrderData(orderData);
            
            if (success) {
                log.info("데이터 플랫폼 전송 성공 - orderId: {}", event.getOrderId());
            } else {
                log.warn("데이터 플랫폼 전송 실패 - orderId: {}", event.getOrderId());
            }
            
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 중 예외 발생 - orderId: {}", event.getOrderId(), e);
            // 실패해도 메인 트랜잭션에 영향 없음
        }
    }


    /**
     * 주문 데이터를 데이터 플랫폼 형식으로 변환
     */
    private DataPlatformOrderResponse convertToDataPlatformFormat(OrderCompletedEvent event) {
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


    // DTO 클래스들
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
