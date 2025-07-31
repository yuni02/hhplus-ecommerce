package kr.hhplus.be.server.coupon.adapter.in.response;

import java.time.LocalDateTime; 
import io.swagger.v3.oas.annotations.media.Schema;      

@Schema(description = "쿠폰 응답")
public class CouponResponse {

    @Schema(description = "사용자 쿠폰 ID", example = "1")
    private Long userCouponId;

    @Schema(description = "쿠폰 ID", example = "1")
    private Long couponId;

    @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
    private String couponName;

    @Schema(description = "할인 금액", example = "1000")
    private Integer discountAmount;

    @Schema(description = "상태", example = "AVAILABLE")
    private String status;

    @Schema(description = "발급일시")
    private LocalDateTime issuedAt;

    public CouponResponse() {}

    public CouponResponse(Long userCouponId, Long couponId, String couponName, 
                         Integer discountAmount, String status, LocalDateTime issuedAt) {
        this.userCouponId = userCouponId;
        this.couponId = couponId;
        this.couponName = couponName;
        this.discountAmount = discountAmount;
        this.status = status;
        this.issuedAt = issuedAt;
    }

    public Long getUserCouponId() {
        return userCouponId;
    }

    public void setUserCouponId(Long userCouponId) {
        this.userCouponId = userCouponId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public String getCouponName() {
        return couponName;
    }

    public void setCouponName(String couponName) {
        this.couponName = couponName;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
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

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
} 