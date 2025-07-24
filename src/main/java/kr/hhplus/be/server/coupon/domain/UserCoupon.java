package kr.hhplus.be.server.coupon.domain;

import java.time.LocalDateTime;

/**
 * 사용자 쿠폰 도메인 엔티티
 * 순수한 비즈니스 로직만 포함
 */
public class UserCoupon {

    private Long id;
    private Long userId;
    private Long couponId;
    private Integer discountAmount; // 할인 금액
    private UserCouponStatus status = UserCouponStatus.AVAILABLE;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private Long orderId; // 사용된 주문 ID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserCoupon() {}

    public UserCoupon(Long userId, Long couponId, Integer discountAmount) {
        this.userId = userId;
        this.couponId = couponId;
        this.discountAmount = discountAmount;
        this.issuedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isAvailable() {
        return status == UserCouponStatus.AVAILABLE;
    }

    public void use(Long orderId) {
        this.status = UserCouponStatus.USED;
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void use(LocalDateTime usedAt) {
        this.status = UserCouponStatus.USED;
        this.usedAt = usedAt;
        this.updatedAt = LocalDateTime.now();
    }

    public enum UserCouponStatus {
        AVAILABLE, USED, EXPIRED
    }
} 