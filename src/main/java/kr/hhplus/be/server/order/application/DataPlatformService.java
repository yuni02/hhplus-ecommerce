package kr.hhplus.be.server.order.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 데이터 플랫폼 전송 서비스 (Mock)
 * 실제 운영에서는 외부 데이터 플랫폼 API와 연동
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPlatformService {

    private final RestTemplate restTemplate;
    
    // Mock API URL (실제로는 설정 파일에서 관리)
    private static final String DATA_PLATFORM_API_URL = "https://api.dataplatform.com/orders";

    /**
     * 주문 데이터를 데이터 플랫폼으로 전송
     * 
     * @param orderData 전송할 주문 데이터
     * @return 전송 성공 여부
     */
    public boolean sendOrderData(OrderCompletedEventHandler.DataPlatformOrderDto orderData) {
        try {
            log.debug("데이터 플랫폼 전송 요청 - orderId: {}", orderData.getOrderId());
            
            // Mock API 호출 (실제로는 RestTemplate 사용)
            // ResponseEntity<String> response = restTemplate.postForEntity(
            //     DATA_PLATFORM_API_URL, orderData, String.class);
            
            // Mock 응답 시뮬레이션
            boolean success = simulateDataPlatformResponse(orderData);
            
            if (success) {
                log.info("데이터 플랫폼 전송 성공 - orderId: {}, userId: {}", 
                        orderData.getOrderId(), orderData.getUserId());
            } else {
                log.warn("데이터 플랫폼 전송 실패 - orderId: {}", orderData.getOrderId());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 중 예외 발생 - orderId: {}", orderData.getOrderId(), e);
            return false;
        }
    }

    /**
     * 데이터 플랫폼 응답 시뮬레이션
     * 실제 운영에서는 제거하고 실제 API 응답 처리
     */
    private boolean simulateDataPlatformResponse(OrderCompletedEventHandler.DataPlatformOrderDto orderData) {
        // 95% 성공률로 시뮬레이션
        double random = Math.random();
        boolean success = random < 0.95;
        
        // 성공 시 약간의 지연 시뮬레이션 (100-500ms)
        if (success) {
            try {
                Thread.sleep(100 + (long)(Math.random() * 400));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return success;
    }
}
