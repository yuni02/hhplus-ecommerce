package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.order.domain.OrderCompletedEvent;
import kr.hhplus.be.server.order.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class OrderCompletedEventHandler {

    private final DataPlatformService dataPlatformService;
    private final NotificationService notificationService;

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
            DataPlatformOrderDto orderData = convertToDataPlatformFormat(event);
            
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
     * 알림톡 발송 핸들러
     * AFTER_COMMIT으로 트랜잭션 완료 후 실행
     */
    @Async("orderEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationSend(OrderCompletedEvent event) {
        try {
            log.info("주문 완료 알림톡 발송 시작 - orderId: {}, userId: {}", 
                    event.getOrderId(), event.getUserId());
            
            // 알림톡 메시지 생성
            NotificationMessageDto message = createOrderCompletionMessage(event);
            
            // 알림톡 발송 (Mock API 호출)
            boolean success = notificationService.sendOrderCompletionNotification(message);
            
            if (success) {
                log.info("주문 완료 알림톡 발송 성공 - orderId: {}", event.getOrderId());
            } else {
                log.warn("주문 완료 알림톡 발송 실패 - orderId: {}", event.getOrderId());
            }
            
        } catch (Exception e) {
            log.error("주문 완료 알림톡 발송 중 예외 발생 - orderId: {}", event.getOrderId(), e);
            // 실패해도 메인 트랜잭션에 영향 없음
        }
    }

    /**
     * 주문 데이터를 데이터 플랫폼 형식으로 변환
     */
    private DataPlatformOrderDto convertToDataPlatformFormat(OrderCompletedEvent event) {
        List<DataPlatformOrderItemDto> items = event.getOrderItems().stream()
                .map(this::convertOrderItem)
                .toList();
        
        return DataPlatformOrderDto.builder()
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
    private DataPlatformOrderItemDto convertOrderItem(OrderItem item) {
        return DataPlatformOrderItemDto.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }

    /**
     * 주문 완료 알림톡 메시지 생성
     */
    private NotificationMessageDto createOrderCompletionMessage(OrderCompletedEvent event) {
        String productNames = event.getOrderItems().stream()
                .map(OrderItem::getProductName)
                .limit(3) // 최대 3개 상품명만 표시
                .reduce((a, b) -> a + ", " + b)
                .orElse("상품");
        
        if (event.getOrderItems().size() > 3) {
            productNames += " 외 " + (event.getOrderItems().size() - 3) + "개";
        }
        
        return NotificationMessageDto.builder()
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .messageType("ORDER_COMPLETION")
                .title("주문이 완료되었습니다")
                .content(String.format("주문번호: %d\n상품: %s\n결제금액: %,d원", 
                        event.getOrderId(), productNames, event.getDiscountedAmount().intValue()))
                .build();
    }

    // DTO 클래스들
    @lombok.Builder
    @lombok.Getter
    public static class DataPlatformOrderDto {
        private final Long orderId;
        private final Long userId;
        private final List<DataPlatformOrderItemDto> orderItems;
        private final BigDecimal totalAmount;
        private final BigDecimal discountedAmount;
        private final BigDecimal discountAmount;
        private final Long userCouponId;
        private final java.time.LocalDateTime orderedAt;
        private final java.time.LocalDateTime occurredAt;
    }

    @lombok.Builder
    @lombok.Getter
    public static class DataPlatformOrderItemDto {
        private final Long productId;
        private final String productName;
        private final Integer quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal totalPrice;
    }

    @lombok.Builder
    @lombok.Getter
    public static class NotificationMessageDto {
        private final Long userId;
        private final Long orderId;
        private final String messageType;
        private final String title;
        private final String content;
    }
}
