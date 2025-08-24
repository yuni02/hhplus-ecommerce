package kr.hhplus.be.server.order.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 알림톡 발송 서비스 (Mock)
 * 실제 운영에서는 외부 알림톡 API와 연동
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RestTemplate restTemplate;
    
    // Mock API URL (실제로는 설정 파일에서 관리)
    private static final String NOTIFICATION_API_URL = "https://api.notification.com/send";

    /**
     * 주문 완료 알림톡 발송
     * 
     * @param message 알림톡 메시지
     * @return 발송 성공 여부
     */
    public boolean sendOrderCompletionNotification(OrderCompletedEventHandler.NotificationMessageDto message) {
        try {
            log.debug("주문 완료 알림톡 발송 요청 - orderId: {}, userId: {}", 
                    message.getOrderId(), message.getUserId());
            
            // Mock API 호출 (실제로는 RestTemplate 사용)
            // ResponseEntity<String> response = restTemplate.postForEntity(
            //     NOTIFICATION_API_URL, message, String.class);
            
            // Mock 응답 시뮬레이션
            boolean success = simulateNotificationResponse(message);
            
            if (success) {
                log.info("주문 완료 알림톡 발송 성공 - orderId: {}, userId: {}", 
                        message.getOrderId(), message.getUserId());
            } else {
                log.warn("주문 완료 알림톡 발송 실패 - orderId: {}", message.getOrderId());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("주문 완료 알림톡 발송 중 예외 발생 - orderId: {}", message.getOrderId(), e);
            return false;
        }
    }

    /**
     * 알림톡 응답 시뮬레이션
     * 실제 운영에서는 제거하고 실제 API 응답 처리
     */
    private boolean simulateNotificationResponse(OrderCompletedEventHandler.NotificationMessageDto message) {
        // 90% 성공률로 시뮬레이션 (알림톡은 데이터 플랫폼보다 실패 가능성 높음)
        double random = Math.random();
        boolean success = random < 0.90;
        
        // 성공 시 약간의 지연 시뮬레이션 (200-800ms)
        if (success) {
            try {
                Thread.sleep(200 + (long)(Math.random() * 600));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return success;
    }
}
