package kr.hhplus.be.server.shared.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueMessage implements Serializable {
    
    private Long userId;
    private Long couponId;
    private String couponName;
    private Integer discountAmount;
    private Integer maxIssuanceCount;
    private LocalDateTime requestedAt;
    
    public static CouponIssueMessage of(Long userId, Long couponId, String couponName, 
                                        Integer discountAmount, Integer maxIssuanceCount) {
        return CouponIssueMessage.builder()
                .userId(userId)
                .couponId(couponId)
                .couponName(couponName)
                .discountAmount(discountAmount)
                .maxIssuanceCount(maxIssuanceCount)
                .requestedAt(LocalDateTime.now())
                .build();
    }
}