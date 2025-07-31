package kr.hhplus.be.server.coupon.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 쿠폰 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    private Long id;
    private String name;
    private String description;
    private BigDecimal discountAmount;
    private Integer maxIssuanceCount;
    
    @Builder.Default
    private Integer issuedCount = 0;
    
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;
    
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 비즈니스 로직 메서드들
    public boolean canIssue() {
        return status == CouponStatus.ACTIVE 
            && issuedCount < maxIssuanceCount
            && (validFrom == null || LocalDateTime.now().isAfter(validFrom))
            && (validTo == null || LocalDateTime.now().isBefore(validTo));
    }

    public void incrementIssuedCount() {
        if (!canIssue()) {
            throw new IllegalStateException("쿠폰을 발급할 수 없습니다.");
        }
        this.issuedCount++;
        this.updatedAt = LocalDateTime.now();
        
        // 발급 수량이 최대치에 도달하면 상태를 SOLD_OUT으로 변경
        if (this.issuedCount >= this.maxIssuanceCount) {
            this.status = CouponStatus.SOLD_OUT;
        }
    }

    public enum CouponStatus {
        ACTIVE, INACTIVE, SOLD_OUT, EXPIRED
    }
} 