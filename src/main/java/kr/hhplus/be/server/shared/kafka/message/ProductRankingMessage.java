package kr.hhplus.be.server.shared.kafka.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품 랭킹 업데이트 Kafka 메시지
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRankingMessage {
    
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Long soldQuantity;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventOccurredAt;
}