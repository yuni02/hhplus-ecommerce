package kr.hhplus.be.server.shared.kafka.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터 플랫폼 전송 Kafka 메시지
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataPlatformMessage {
    
    private Long orderId;
    private Long userId;
    private String userName;
    private BigDecimal totalAmount;
    private BigDecimal discountedAmount;
    private BigDecimal discountAmount;
    private String orderStatus;
    private String paymentMethod;
    private List<DataPlatformOrderItem> orderItems;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataPlatformOrderItem {
        private Long productId;
        private String productName;
        private String category;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}