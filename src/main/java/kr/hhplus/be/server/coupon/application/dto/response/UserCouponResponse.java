package kr.hhplus.be.server.coupon.application.dto.response;

import java.time.LocalDateTime;

public class UserCouponResponse {

    private final Long userCouponId;
    private final Long couponId;
    private final String couponName;
    private final Integer discountAmount;
    private String status;
    private final LocalDateTime issuedAt;
    private LocalDateTime usedAt;

    public UserCouponResponse(Long userCouponId, Long couponId, String couponName,
            Integer discountAmount, String status,
            LocalDateTime issuedAt, LocalDateTime usedAt) {
        this.userCouponId = userCouponId;
        this.couponId = couponId;
        this.couponName = couponName;
        this.discountAmount = discountAmount;
        this.status = status;
        this.issuedAt = issuedAt;
        this.usedAt = usedAt;
    }

    public Long getUserCouponId() {
        return userCouponId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public String getCouponName() {
        return couponName;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
} 