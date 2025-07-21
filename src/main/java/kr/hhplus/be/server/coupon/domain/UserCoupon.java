package kr.hhplus.be.server.coupon.domain;

import kr.hhplus.be.server.shared.domain.BaseEntity;

import java.time.LocalDateTime;

public class UserCoupon extends BaseEntity {

    private Long userId;
    private Long couponId;
    private UserCouponStatus status = UserCouponStatus.AVAILABLE;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private Long orderId; // 사용된 주문 ID

    public UserCoupon() {}

    public UserCoupon(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
        this.issuedAt = LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public UserCouponStatus getStatus() {
        return status;
    }

    public void setStatus(UserCouponStatus status) {
        this.status = status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public boolean isAvailable() {
        return status == UserCouponStatus.AVAILABLE;
    }

    public void use(Long orderId) {
        if (!isAvailable()) {
            throw new IllegalStateException("사용할 수 없는 쿠폰입니다.");
        }
        this.status = UserCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.orderId = orderId;
    }

    public enum UserCouponStatus {
        AVAILABLE, USED, EXPIRED
    }
} 