package kr.hhplus.be.server.coupon.domain;

import kr.hhplus.be.server.user.domain.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자 쿠폰 도메인 엔티티
 * 순수한 비즈니스 로직만 포함 (JPA 어노테이션 없음)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCoupon {

    private Long id;
    private Long userId;
    private Long couponId;
    private Integer discountAmount; // 할인 금액
    
    @Builder.Default
    private UserCouponStatus status = UserCouponStatus.AVAILABLE;
    
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private Long orderId; // 사용된 주문 ID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private User user;
    private Coupon coupon;

    // 비즈니스 로직 메서드들
    public boolean isAvailable() {
        return status == UserCouponStatus.AVAILABLE;
    }

    public void use(Long orderId) {
        if (!isAvailable()) {
            throw new IllegalStateException("사용할 수 없는 쿠폰입니다.");
        }
        this.status = UserCouponStatus.USED;
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void use(LocalDateTime usedAt) {
        if (!isAvailable()) {
            throw new IllegalStateException("사용할 수 없는 쿠폰입니다.");
        }
        this.status = UserCouponStatus.USED;
        this.usedAt = usedAt;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 사용 취소 (복원)
     */
    public void restore() {
        if (status != UserCouponStatus.USED) {
            throw new IllegalStateException("사용된 쿠폰만 복원할 수 있습니다.");
        }
        this.status = UserCouponStatus.AVAILABLE;
        this.usedAt = null;
        this.orderId = null;
        this.updatedAt = LocalDateTime.now();
    }

    public enum UserCouponStatus {
        AVAILABLE, USED, EXPIRED
    }
} 